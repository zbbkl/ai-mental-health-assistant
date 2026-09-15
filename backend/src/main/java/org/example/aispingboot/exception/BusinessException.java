package org.example.aispingboot.exception;

import lombok.Getter;
import org.example.aispingboot.common.ResultCode;

/**
 * 业务异常类
 * @author system
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final String message;
    private final Object data;

    public BusinessException(String message) {
        super(message);
        // 与 ResultCode 保持同一套编码，避免出现枚举里查不到的 "BUSINESS_ERROR" 这种孤立取值
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.message = message;
        this.data = null;
    }

}