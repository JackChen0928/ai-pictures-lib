package com.web.aipictureslib.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 统一处理所有业务异常
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 统一处理所有运行时异常
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NOT_AUTH_ERROR, e.getMessage());
    }

}
