package com.web.aipictureslib.common;

import com.web.aipictureslib.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
//数据是返回给前端的，需要序列化
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}


