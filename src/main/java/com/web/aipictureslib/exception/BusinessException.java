package com.web.aipictureslib.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    /**
	 * 错误码
	 */
    private final int code;

    /**
	 * 错误码枚举
     * 手动传入错误码（code）和错误信息（message）。
     * 调用父类 RuntimeException 的构造方法传入提示信息。
     * 当你需要自定义错误码和提示信息时使用，灵活性高。
	 */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
	 * 错误码枚举
     * 直接通过 ErrorCode 枚举创建异常。
     * 自动从枚举中提取 code 和 message。
     * 统一使用枚举管理错误码和消息，增强可维护性。
	 */
    public BusinessException(ErrorCode errorCode) {
       super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    /**
	 * 错误码枚举
     * 通过 ErrorCode 枚举创建异常，并传入错误信息。
     * 自动从枚举中提取 code，并使用传入的 message。
     * 适用于某些错误码需要特殊提示信息的场景。
     * 例如：throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
	 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}
