package cn.czyx007.filemanage.service.impl;

import cn.czyx007.filemanage.bean.OSS;
import cn.czyx007.filemanage.mapper.OSSMapper;
import cn.czyx007.filemanage.service.OSSService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OSSServiceImpl extends ServiceImpl<OSSMapper, OSS> implements OSSService {
    @Override
    public OSS getActive(){
        return getOne(new LambdaQueryWrapper<OSS>().eq(OSS::getIsActive, true));
    }
}
