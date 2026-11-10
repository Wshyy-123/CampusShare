package com.campusshare.common;

import com.campusshare.lifecycle.LifecycleException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException exception) {
        return error(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(LifecycleException.class)
    public ResponseEntity<ApiResponse<Void>> lifecycle(LifecycleException exception) {
        return error(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> invalidBody(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null
                ? "请求参数不完整"
                : fieldError.getField() + " " + Objects.requireNonNullElse(fieldError.getDefaultMessage(), "不合法");
        return error(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", "请求体格式不正确");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT",
                "参数 " + exception.getName() + " 格式不正确");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> authentication(AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED.value(), "AUTH_REQUIRED", "账号或密码错误");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> duplicateKey(DuplicateKeyException exception) {
        return error(HttpStatus.CONFLICT.value(), "IDEMPOTENCY_CONFLICT", "请求与已有记录冲突");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        LOG.error("Unhandled request failure: requestId={}", RequestIdContext.get(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "系统暂时无法处理，请稍后重试");
    }

    private static ResponseEntity<ApiResponse<Void>> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
