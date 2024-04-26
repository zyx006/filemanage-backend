package cn.czyx007.filemanage.common;

import cn.czyx007.filemanage.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//全局异常处理
@RestControllerAdvice(annotations = RestController.class)
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(CustomException e){
        log.error(e.toString());
        return Result.error(e.getMessage());
    }
}
