package org.example.aispingboot.common;

import lombok.extern.slf4j.Slf4j;
import org.example.aispingboot.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobarExceptionHandler {
    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handlerException(MethodArgumentNotValidException e) {
        // 处理异常数据的处理
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), message);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 如果异样携带额外的数据
        if (e.getData() != null) {
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        return Result.error(e.getCode(), e.getMessage(), null);
    }

    // 请求体格式错误（JSON 语法错误、字段类型不匹配等）
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_INVALID.getCode(), ResultCode.PARAM_INVALID.getMsg(),
                "请求体格式不正确，请检查字段类型");
    }

    // 静态资源不存在时保持 404，避免被下面的兜底处理器改写成 200
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<String>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMsg(), e.getResourcePath()));
    }

    // 兜底处理其它未预期异常
    // 没有这层兜底时异常会转发到容器错误页，被安全过滤链改写成无响应体的 403，前端只能看到“打不开”
    @ExceptionHandler(Exception.class)
    public Result<String> handleUnexpectedException(Exception e) {
        log.error("接口处理异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMsg(), e.getMessage());
    }
}
