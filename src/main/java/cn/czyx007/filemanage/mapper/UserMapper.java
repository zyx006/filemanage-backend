package cn.czyx007.filemanage.mapper;

import cn.czyx007.filemanage.bean.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * 用户信息(User)表数据库访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
