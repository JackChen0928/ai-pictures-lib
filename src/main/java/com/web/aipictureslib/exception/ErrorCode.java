package com.web.aipictureslib.exception;

import lombok.Getter;

//枚举类
@Getter
public enum ErrorCode {

    /**
     * 错误码枚举
     */
    SUCCESS(0, "ok"),//调用构造方法， 相当于 new ErrotCode(0, "ok");
    PARAM_ERROR(40000, "参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NOT_AUTH_ERROR(40300, "无权限"),
    NOT_FOUND_ERROR(40400, "未找到"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败");



    /**
     * 错误码
     */
    private final int code;
    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误信息
     */
    ErrorCode(int code, String message){
        this.code = code;
        this.message = message;
    }
}
