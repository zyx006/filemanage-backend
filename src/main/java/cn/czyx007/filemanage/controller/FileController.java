package cn.czyx007.filemanage.controller;

import cn.czyx007.filemanage.bean.Files;
import cn.czyx007.filemanage.bean.OSS;
import cn.czyx007.filemanage.service.FilesService;
import cn.czyx007.filemanage.service.OSSService;
import cn.czyx007.filemanage.service.UserService;
import cn.czyx007.filemanage.utils.BaseContext;
import cn.czyx007.filemanage.utils.COSUtil;
import cn.czyx007.filemanage.utils.Result;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    private static String uploadPath;
    @Autowired
    private FilesService filesService;
    @Autowired
    private OSSService ossService;
    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${oss.uploadPath}")
    public void setUploadPath(String uploadPath) {
        FileController.uploadPath = uploadPath;
    }

    @GetMapping("/page/{page}/{pageSize}/{isDelete}")
    public Result<IPage<Files>> list(@PathVariable("page") Integer page, @PathVariable("pageSize") Integer pageSize,
                                     @PathVariable("isDelete") Integer isDelete, @RequestParam("name") String name) {
        String key = "fileCache::" + BaseContext.getCurrentId() + "_" + page + "_" + pageSize + "_" + isDelete + "_" + name;
        String res = redisTemplate.opsForValue().get(key);
        if (res != null){
            IPage resPage = JSON.parseObject(res, IPage.class);
            return Result.success(resPage);
        }

        IPage<Files> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Files> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Files::getUserId, BaseContext.getCurrentId()).eq(Files::getIsDelete, isDelete);
        if(name != null && !name.isEmpty())
            lqw.like(Files::getName, name);
        filesService.page(iPage, lqw);

        redisTemplate.opsForValue().set(key, JSON.toJSONString(iPage));
        return Result.success(iPage);
    }

//    @GetMapping("/{id}")
//    public Result<Files> getFile(@PathVariable("id") String id){
//        Files file = filesService.getFileById(id);
//        if(file == null)
//            return Result.error("文件不存在或被删除");
//        return Result.success(file);
//    }

    @GetMapping("/preview/{id}")
    public Result<String> getPreviewFile(@PathVariable("id") String id){
        String key = "previewFile::"+BaseContext.getCurrentId()+"_"+id;
        Files file = filesService.getById(id);
        if(file == null) {
            redisTemplate.delete(key);
            return Result.error("文件不存在或被删除");
        } else {
            String res = redisTemplate.opsForValue().get(key);
            if (res != null)
                return Result.success(res);
        }

        //云存储，直接返回url
        if(file.getPath().startsWith("http")) {
            redisTemplate.opsForValue().set(key, file.getPath());
            return Result.success(file.getPath());
        }
        // 本地存储，读取图片文件内容
        Path imagePath = Paths.get(file.getPath());
        byte[] imageBytes;
        try {
            imageBytes = java.nio.file.Files.readAllBytes(imagePath);
        } catch (IOException e) {
            log.error("读取图片文件内容失败", e);
            return Result.error(e.getMessage());
        }

        // 将图片的内容转换为 base64 格式
        String suffix = file.getName().substring(file.getName().lastIndexOf(".")+1);
        String base64Image = "data:image/"+suffix+";base64,"+Base64.getEncoder().encodeToString(imageBytes);
        redisTemplate.opsForValue().set(key, base64Image);
        return Result.success(base64Image);
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile multipartFile){
        if (multipartFile == null || multipartFile.isEmpty()) {
            return Result.error("未上传文件");
        }
        OSS ossConfig = ossService.getActive();
        if (ossConfig == null) {
            return Result.error("没有有效的存储策略");
        }

        //更新用户已用空间
        if(!userService.updateStorageUsed(multipartFile.getSize(), true))
            return Result.error("空间不足");

        try {
            String fileUploadPath = System.getProperty("user.dir") + uploadPath;

            // 处理文件上传逻辑，根据存储策略来保存文件
            Files file = new Files();

            // 设置文件名
            file.setName(multipartFile.getOriginalFilename());

            // 设置存储名（UUID 形式，保证唯一性）
            String uuid = UUID.randomUUID().toString();
            String originName = multipartFile.getOriginalFilename();
            String suffix = originName.substring(originName.lastIndexOf("."));
            String storeName = uuid+suffix;
            file.setStoreName(storeName);
            log.info(file.toString());

            // 设置文件存储路径
            switch (ossConfig.getType()){
                case 0://阿里云

                    break;
                case 1://腾讯云
                    COSUtil.init(ossConfig);
                    file.setPath(COSUtil.customUrl + BaseContext.getCurrentId() + "/" + storeName);
                    break;
                case 2://七牛云

                    break;
                case 3://本地
                    String filePath = fileUploadPath + "/" + storeName;
                    file.setPath(filePath);
                    break;
            }
            log.info("上传文件保存路径：" + file.getPath());

            // 设置文件大小
            file.setSize(multipartFile.getSize());

            // 设置文件类型
            int fileType;
            String mimeType = java.nio.file.Files.probeContentType(Paths.get(storeName));
            if (mimeType != null) {
                if (mimeType.startsWith("image/")) {
                    fileType = 0; // 图片类型
                } else if (mimeType.startsWith("audio/")) {
                    fileType = 1; // 音频类型
                } else if (mimeType.startsWith("video/")) {
                    fileType = 2; // 视频类型
                } else if (mimeType.startsWith("text/") || mimeType.endsWith("pdf") ||
                        mimeType.startsWith("application/vnd.openxmlformats-officedocument") ||
                        mimeType.equals("application/pdf") || mimeType.endsWith("json")) {
                    fileType = 3; // 文档类型
                } else {
                    fileType = 4; // 其他类型
                }
            } else {
                fileType = 4; // 无法确定类型，设置为其他类型
            }
            file.setType(fileType);

            // 设置所属用户ID
            String userId = BaseContext.getCurrentId();
            file.setUserId(userId);

            //设置存储策略id
            file.setOssId(ossConfig.getId());

            // 保存文件到指定目录
            switch (ossConfig.getType()){
                case 0://阿里云
                    break;
                case 1://腾讯云
                    COSUtil.uploadFile(multipartFile, file);
                    break;
                case 2://七牛云
                    break;
                case 3://本地
                    File tmpFile = new File(fileUploadPath, storeName);
                    // 检查目录是否存在，如果不存在则创建目录
                    if (!tmpFile.getParentFile().exists()) {
                        tmpFile.getParentFile().mkdirs(); // 创建目录及其父目录
                    }
                    multipartFile.transferTo(tmpFile);
                    break;
                default:
                    break;
            }

            // 调用文件服务保存文件信息到数据库
            filesService.save(file);
            //清除该用户的文件列表缓存
            Set keys = redisTemplate.keys("fileCache::" + BaseContext.getCurrentId() + "*");
            if (keys != null) {
                redisTemplate.delete(keys);
            }

            return Result.success("文件上传成功");
        } catch (Exception e) {
            log.error("文件上传失败，", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") String id) {
        Files file = filesService.getFileById(id);
        if (file == null) {
            return ResponseEntity.status(404).body(null);
        }
        // 设置响应头信息
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

        // 创建文件资源对象并返回
        Resource resource = null;
        if(file.getPath().startsWith("http")){
            try {
                resource = new UrlResource(file.getPath());
            } catch (MalformedURLException e) {
                log.error(e.toString());
            }
        } else {
            resource = new FileSystemResource(new File(file.getPath()));
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @PutMapping("/{id}/{isDelete}")
    public Result<String> updateIsDelete(@PathVariable("id") String id, @PathVariable("isDelete") Integer isDelete){
        //清除该用户的文件列表缓存
        Set keys = redisTemplate.keys("fileCache::" + BaseContext.getCurrentId() + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        //清除该用户的该文件预览缓存
        redisTemplate.delete("previewFile::"+BaseContext.getCurrentId()+"_"+id);

        Files file = filesService.getById(id);
        if(file == null) {
            return Result.error("文件不存在或被删除");
        }
        if(isDelete == 1){
            file.setIsDelete(1);
            filesService.updateById(file);
            return Result.success("文件删除成功");
        } else if(isDelete == 0){
            file.setIsDelete(0);
            filesService.updateById(file);
            return Result.success("文件恢复成功");
        } else return Result.error("参数错误");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") String id){
        //清除该用户的文件列表缓存
        Set keys = redisTemplate.keys("fileCache::" + BaseContext.getCurrentId() + "*");
        if(keys != null)
            redisTemplate.delete(keys);

        Files file = filesService.getById(id);
        if(file == null)
            return Result.error("文件不存在或被删除");
        if(file.getIsDelete() == 0) {
            return Result.error("你不能永久删除未在回收站的文件");
        }
        //物理删除
        filesService.removeById(id);
        if(file.getPath().startsWith("http")){
            COSUtil.init(ossService.getById(file.getOssId()));
            COSUtil.createCOSClient().deleteObject(COSUtil.bucketName, BaseContext.getCurrentId()+"/"+file.getStoreName());
        } else {
            new File(file.getPath()).delete();
        }
        //更新用户已用存储空间
        userService.updateStorageUsed(file.getSize(), false);

        return Result.success("文件已被彻底删除");
    }

    @PutMapping("/{isDelete}")
    public Result<String> batchUpdateIsDelete(@PathVariable("isDelete") Integer isDelete, @RequestBody List<String> ids){
        //清除该用户的文件列表缓存
        Set keys = redisTemplate.keys("fileCache::" + BaseContext.getCurrentId() + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        //清除该用户的该文件预览缓存
        keys = redisTemplate.keys("previewFile::" + BaseContext.getCurrentId() + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }

        List<Files> list = filesService.listByIds(ids);
        if(list == null) {
            return Result.error("文件不存在或被删除");
        }

        if(isDelete == 1){
            list.forEach(file -> file.setIsDelete(1));
            filesService.updateBatchById(list);
            return Result.success("文件批量删除成功");
        } else if(isDelete == 0){
            list.forEach(file -> file.setIsDelete(0));
            filesService.updateBatchById(list);
            return Result.success("文件批量恢复成功");
        } else return Result.error("参数错误");
    }

    @DeleteMapping
    public Result<String> batchDelete(@RequestBody List<String> ids){
        //清除该用户的文件列表缓存
        Set keys = redisTemplate.keys("fileCache::" + BaseContext.getCurrentId() + "*");
        if(keys != null)
            redisTemplate.delete(keys);

       List<Files> list = filesService.listByIds(ids);
        if(list == null)
            return Result.error("文件不存在或被删除");

        long updateSize = 0;
        for(Files file : list) {
            if (file.getIsDelete() == 0) {
                return Result.error("你不能永久删除未在回收站的文件");
            }
            updateSize += file.getSize();
        }
        //物理删除
        filesService.removeBatchByIds(ids);

        for (Files file : list) {
            if(file.getPath().startsWith("http")){
                COSUtil.init(ossService.getById(file.getOssId()));
                COSUtil.createCOSClient().deleteObject(COSUtil.bucketName, BaseContext.getCurrentId()+"/"+file.getStoreName());
            } else {
                new File(file.getPath()).delete();
            }
        }
        //更新用户已用存储空间
        userService.updateStorageUsed(updateSize, false);

        return Result.success("文件已被彻底删除");
    }
}
