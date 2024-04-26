package cn.czyx007.filemanage.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class EncryptUtils {
    public static String hashPassword(String password) {
        try {
            // 创建 MessageDigest 实例并指定算法为 SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            // 将密码转换为字节数组
            byte[] passwordBytes = password.getBytes();
            // 使用 MessageDigest 更新字节数组
            byte[] hashedBytes = md.digest(passwordBytes);

            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 处理算法不存在异常
            log.error("密码加密失败：", e);
            return null;
        }
    }
}
