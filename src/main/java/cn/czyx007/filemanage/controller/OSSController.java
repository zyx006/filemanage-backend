package cn.czyx007.filemanage.controller;

import cn.czyx007.filemanage.bean.Files;
import cn.czyx007.filemanage.bean.OSS;
import cn.czyx007.filemanage.bean.User;
import cn.czyx007.filemanage.service.FilesService;
import cn.czyx007.filemanage.service.OSSService;
import cn.czyx007.filemanage.service.UserService;
import cn.czyx007.filemanage.utils.BaseContext;
import cn.czyx007.filemanage.utils.Result;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oss")
public class OSSController {
    @Autowired
    private UserService userService;
    @Autowired
    private OSSService ossService;
    @Autowired
    private FilesService filesService;

    @GetMapping
    public Result<List<OSS>> list(){
        if(userService.getById(BaseContext.getCurrentId()).getIsAdmin() == 0)
            return Result.error("无权限");

        List<OSS> list = ossService.list();
        Map<String, String> idToName = new HashMap<>();

        // 获取所有管理员用户并将其ID与用户名存入idToName映射中
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getIsAdmin, 1);
        List<User> adminUsers = userService.list(lqw);
        for (User adminUser : adminUsers) {
            idToName.put(adminUser.getId(), adminUser.getUsername());
        }

        // 更新OSS对象中的创建者和更新者信息
        for (OSS oss : list) {
            String creator = oss.getCreator();
            if (!"-1".equals(creator)) {
                String username = idToName.get(creator);
                if (username != null) {
                    oss.setCreator(username);
                }
            }

            String updater = oss.getUpdater();
            if (!"-1".equals(updater)) {
                String username = idToName.get(updater);
                if (username != null) {
                    oss.setUpdater(username);
                }
            }
        }

        return Result.success(list);
    }

    @PostMapping
    public Result<String> addConfig(@RequestBody Map<String,String> config){
        if(userService.getById(BaseContext.getCurrentId()).getIsAdmin() == 0)
            return Result.error("无权限");
        OSS oss = new OSS();
        oss.setName(config.get("name"));
        oss.setType(Integer.valueOf(config.get("type")));

        config.remove("name");
        config.remove("type");
        oss.setConfig(JSON.toJSONString(config));

        oss.setCreator(BaseContext.getCurrentId());
        oss.setUpdater(BaseContext.getCurrentId());
        ossService.save(oss);
        return Result.success("存储配置保存成功");
    }

    @PutMapping("/{id}")
    public Result<String> updateConfig(@RequestBody Map<String, String> config, @PathVariable("id") String id){
        if(userService.getById(BaseContext.getCurrentId()).getIsAdmin() == 0)
            return Result.error("无权限");
        OSS oss = ossService.getById(id);
        oss.setName(config.get("name"));
        oss.setUpdater(BaseContext.getCurrentId());

        config.remove("name");
        config.remove("type");
        JSONObject storageConfig = JSON.parseObject(oss.getConfig());
        if(storageConfig != null) {
            storageConfig.put("customUrl", config.get("customUrl"));
            oss.setConfig(JSON.toJSONString(storageConfig));
        } else {
            oss.setConfig(JSON.toJSONString(config));
        }

        ossService.updateById(oss);
        return Result.success("存储配置修改成功");
    }

    @PutMapping("/{id}/{isActive}")
    public Result<String> updateConfigStatus(@PathVariable("id") String id, @PathVariable("isActive") Integer isActive){
        if(userService.getById(BaseContext.getCurrentId()).getIsAdmin() == 0)
            return Result.error("无权限");
        OSS oss = ossService.getById(id);
        if(oss == null)
            return Result.error("存储配置不存在");

        //启用指定存储策略，将其他策略禁用
        if(isActive == 0) {
            oss.setIsActive(1);
            oss.setUpdater(BaseContext.getCurrentId());
            ossService.updateById(oss);

            LambdaUpdateWrapper<OSS> luw = new LambdaUpdateWrapper<>();
            luw.ne(OSS::getId, id).set(OSS::getIsActive, 0);
            ossService.update(luw);
            return Result.success("存储策略启用成功");
        } else {
            //试图仅仅禁用某一个策略，未知要启用的存储策略
            //避免误操作导致没有有效的存储策略
            return Result.error("不允许直接禁用某项存储策略");
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteConfig(@PathVariable("id") String id){
        if(userService.getById(BaseContext.getCurrentId()).getIsAdmin() == 0)
            return Result.error("无权限");
        LambdaQueryWrapper<Files> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Files::getOssId, id);
        if(filesService.exists(lqw)){
            return Result.error("该存储策略下存在文件，无法删除");
        }
        ossService.removeById(id);
        return Result.success("存储配置删除成功");
    }
}
