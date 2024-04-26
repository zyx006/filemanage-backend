package cn.czyx007.filemanage.service.impl;

import cn.czyx007.filemanage.bean.Files;
import cn.czyx007.filemanage.mapper.FilesMapper;
import cn.czyx007.filemanage.service.FilesService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 文件信息(File)表服务实现类
 */
@Service
public class FilesServiceImpl extends ServiceImpl<FilesMapper, Files> implements FilesService {
    @Override
    public Files getFileById(String id){
        LambdaQueryWrapper<Files> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Files::getId, id).eq(Files::getIsDelete, 0);
        return this.getOne(lqw);
    }
}
