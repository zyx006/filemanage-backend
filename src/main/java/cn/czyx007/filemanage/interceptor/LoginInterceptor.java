package cn.czyx007.filemanage.interceptor;

import cn.czyx007.filemanage.utils.BaseContext;
import cn.czyx007.filemanage.utils.Result;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取cookie['user']
        String userId = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("user".equals(cookie.getName())) {
                    userId = cookie.getValue();
                    break;
                }
            }
        }
        log.info("LoginInterceptor >> "+request.getRequestURI()+" >> cookie['user'] = " + userId);
        //判断是否为空
        if (userId != null) {
            log.info("当前用户已经登录，用户id为：{}", userId);
            BaseContext.setCurrentId(userId);
            return true;
        }
        //未登录，响应数据
        response.getWriter().write(JSON.toJSONString(Result.error("NOT-LOGIN")));
        return false;
    }
}
