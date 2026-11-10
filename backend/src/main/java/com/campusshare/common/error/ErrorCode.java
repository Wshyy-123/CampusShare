package com.campusshare.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    BAD_REQUEST("BAD_REQUEST", "请求内容不正确", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败", HttpStatus.BAD_REQUEST),
    INVALID_BOOKING_UNIT("INVALID_BOOKING_UNIT", "资源类型与预约单位不匹配", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "账号或密码错误", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "没有权限执行该操作", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("INTERNAL_ERROR", "服务暂时不可用", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
