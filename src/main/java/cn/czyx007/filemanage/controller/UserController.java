package cn.czyx007.filemanage.controller;

import cn.czyx007.filemanage.bean.User;
import cn.czyx007.filemanage.dto.UserDto;
import cn.czyx007.filemanage.service.UserService;
import cn.czyx007.filemanage.utils.*;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    //发送注册验证码
    @PostMapping("/sendVerCode")
    public Result<String> sendVerCode(@RequestBody Map<String,String> map){
        //生成6位随机数字验证码
        String code = ValidateCodeUtils.generateValidateCode(6).toString();
        log.info("验证码：{}", code);
        //发送短信，让用户接受验证码
        try {
            String email = map.get("email");
            SendEmailUtils.sendAuthCodeEmail(email, code);
            //把验证码保存到redis，5分钟有效
            redisTemplate.opsForValue().set(email + ":code", code, 5, TimeUnit.MINUTES);
            return Result.success("验证码发送成功，请查看邮箱");
        } catch (EmailException e) {
            log.error(e.toString());
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    @GetMapping("/captcha")
    public Result<String> sendCaptcha() {
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        String verCode = specCaptcha.text().toLowerCase();
        String key = UUID.randomUUID().toString();
        // 存入redis并设置过期时间为1分钟
        redisTemplate.opsForValue().set(key, verCode, 1, TimeUnit.MINUTES);
        // 将key和base64返回给前端
        HashMap<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("image", specCaptcha.toBase64());
        log.info("验证码：{}, key:{}", specCaptcha.text(), key);
        return Result.success(JSON.toJSONString(map));
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody UserDto userDto, HttpServletResponse response){
        log.info("UserDto: {}", userDto);
        // 获取redis中的验证码
        String redisCode = redisTemplate.opsForValue().get(userDto.getVerKey());
        if(redisCode==null){
            return Result.error("验证码已过期，请刷新");
        }
        // 校验验证码
        String captcha = userDto.getCaptcha();
        if (captcha==null || !captcha.toLowerCase().equalsIgnoreCase(redisCode)) {
            return Result.error("验证码错误");
        }

        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getEmail, userDto.getEmail())
                .eq(User::getPassword, EncryptUtils.hashPassword(userDto.getPassword()))
                .or(i -> i.eq(User::getUsername, userDto.getEmail())
                        .eq(User::getPassword, EncryptUtils.hashPassword(userDto.getPassword())));

        User user = userService.getOne(lqw);
        if (userService.getOne(lqw)==null){
            return Result.error("账号不存在或密码错误");
        }

        Cookie cookie = new Cookie("user", String.valueOf(user.getId()));
        cookie.setPath("/");
        cookie.setDomain("localhost");
//        cookie.setHttpOnly(true);

        response.addCookie(cookie);
        BaseContext.setCurrentId(user.getId());
        log.info("用户登录成功，用户id:{}", user.getId());
        log.info("cookie:{}",cookie.getValue());

        //验证码使用之后，从redis中删除
        redisTemplate.delete(userDto.getVerKey());
        return Result.success("登录成功");
    }

    @PostMapping("/registry")
    public Result<String> registry(@RequestBody UserDto user){
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getEmail, user.getEmail());
        if (userService.getOne(lqw)!=null){
            return Result.error("该邮箱已被注册");
        }
        // 获取redis中的验证码
        String redisCode = redisTemplate.opsForValue().get(user.getEmail()+":code");
        if(redisCode==null){
            return Result.error("验证码已过期，请重新获取");
        }
        // 校验验证码
        String verCode = user.getVerificationCode();
        if (verCode==null || !verCode.equals(redisCode)) {
            return Result.error("验证码错误");
        }

        user.setUsername(user.getEmail());
        user.setPassword(EncryptUtils.hashPassword(user.getPassword()));
        userService.save(user);
        log.info("用户注册成功：{}", user);

        //验证码使用之后，从redis中删除
        redisTemplate.delete(user.getEmail() + ":code");
        return Result.success("注册成功");
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletResponse response){
        BaseContext.setCurrentId(null);
        Cookie cookie = new Cookie("user", null);
        cookie.setPath("/");
        cookie.setDomain("localhost");
//        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);

        response.addCookie(cookie);
        return Result.success("退出成功");
    }

    @GetMapping
    public Result<User> getUser(){
        String userId = BaseContext.getCurrentId();
        log.info("当前用户id:{}", userId);
        User user = userService.getById(userId);
        if(user==null){
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/updatePassword")
    public Result<String> updatePassword(@RequestBody Map<String,String> map, HttpServletResponse response){
        String oldPassword = map.get("oldPassword");
        String newPassword = map.get("newPassword");
        User user = userService.getById(BaseContext.getCurrentId());
        if(user==null){
            return Result.error("用户不存在");
        }
        if(!user.getPassword().equals(EncryptUtils.hashPassword(oldPassword))){
            return Result.error("旧密码错误");
        }
        user.setPassword(EncryptUtils.hashPassword(newPassword));
        userService.updateById(user);

        //密码修改成功,删除cookie，将用户踢下线
        BaseContext.setCurrentId(null);
        Cookie cookie = new Cookie("user", null);
        cookie.setPath("/");
        cookie.setDomain("localhost");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        log.info("用户密码修改成功，用户id:{}", user.getId());
        return Result.success("密码修改成功");
    }

    @PutMapping("/updateUsername")
    public Result<String> updateUsername(@RequestBody User user){
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUsername, user.getUsername());
        if (userService.getOne(lqw)!=null){
            return Result.error("该用户名已被使用");
        }
        user.setId(BaseContext.getCurrentId());
        userService.updateById(user);
        log.info("用户修改用户名成功，用户id:{}", user.getId());
        return Result.success("用户名修改成功");
    }
}
