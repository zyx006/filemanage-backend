package cn.czyx007.filemanage.service;

import cn.czyx007.filemanage.bean.OSS;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OSSService extends IService<OSS> {
    OSS getActive();
}
