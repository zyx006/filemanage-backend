package cn.czyx007.filemanage.mapper;

import cn.czyx007.filemanage.bean.Files;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息(File)表数据库访问层
 */
@Mapper
public interface FilesMapper extends BaseMapper<Files> {
}
