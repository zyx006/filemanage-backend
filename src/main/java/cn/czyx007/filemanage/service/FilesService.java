package cn.czyx007.filemanage.service;

import cn.czyx007.filemanage.bean.Files;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 文件信息(File)表服务接口
 */
public interface FilesService extends IService<Files> {
    /**
     * 查询id对应的未被删除的文件
     * @param id 文件唯一id
     * @return 文件对象Files
     */
    Files getFileById(String id);
}
