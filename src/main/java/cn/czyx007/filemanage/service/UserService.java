package cn.czyx007.filemanage.service;

import cn.czyx007.filemanage.bean.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户信息(User)表服务接口
 */
public interface UserService extends IService<User> {
    /**
     * 更新用户已使用存储空间
     * @param updateSize 变化的存储空间大小
     * @param isAdd 是否增加
     * @return true:更新成功;false:更新失败(用户剩余存储空间不足)
     */
    boolean updateStorageUsed(long updateSize, boolean isAdd);
}
