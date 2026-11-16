package com.campusshare.lifecycle;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = ResourceDisableController.class)
public class LifecycleControllerAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleControllerAdvice.class);

    @ExceptionHandler(LifecycleException.class)
    public ResponseEntity<ResourceDisableController.Response> business(LifecycleException e) {
        return response(e.status(), e.code(), e.getMessage(), UUID.randomUUID().toString());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ResourceDisableController.Response> invalid(Exception e) {
        return response(400, "INVALID_ARGUMENT", "请检查资源、带时区的起止时间和停用原因", UUID.randomUUID().toString());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResourceDisableController.Response> unexpected(Exception e) {
        var requestId = UUID.randomUUID().toString();
        LOG.error("Resource disable failed: requestId={}", requestId, e);
        return response(500, "INTERNAL_ERROR", "系统暂时无法处理，请稍后重试", requestId);
    }

    private static ResponseEntity<ResourceDisableController.Response> response(int status, String code,
                                                                              String message, String requestId) {
        return ResponseEntity.status(status).body(new ResourceDisableController.Response(code, message, null, requestId));
    }
}
