package cn.czyx007.filemanage.service.impl;

import cn.czyx007.filemanage.bean.User;
import cn.czyx007.filemanage.mapper.UserMapper;
import cn.czyx007.filemanage.service.UserService;
import cn.czyx007.filemanage.utils.BaseContext;
import cn.czyx007.filemanage.utils.Result;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户信息(User)表服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public boolean updateStorageUsed(long updateSize, boolean isAdd) {
        User user = getById(BaseContext.getCurrentId());
        if(isAdd) {
            long newSize = updateSize + user.getStorageUsed();
            //检查是否有足够空间用于存储
            if(newSize > user.getStorageTotal())
                return false;
            user.setStorageUsed(newSize);
        }
        else
            user.setStorageUsed(user.getStorageUsed() - updateSize);
        updateById(user);
        return true;
    }
}
