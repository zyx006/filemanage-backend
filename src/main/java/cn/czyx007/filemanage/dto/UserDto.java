package cn.czyx007.filemanage.dto;

import cn.czyx007.filemanage.bean.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto extends User {
    /**
     * 确认密码
     */
    private String confirmPassword;

    /**
     * 邮箱验证码
     */
    private String verificationCode;

    /**
     * 图片验证码
     */
    private String captcha;

    /**
     * 图片验证码对应key
     */
    private String verKey;

    @Override
    public String toString() {
        return "UserDto{" +
                "confirmPassword='" + confirmPassword + '\'' +
                ", verificationCode='" + verificationCode + '\'' +
                ", captcha='" + captcha + '\'' +
                ", verKey='" + verKey + '\'' +
                ", " + super.toString() + '\'' +
                "} ";
    }
}
