package org.campus.campussharebackend.common;

import org.springframework.http.HttpStatus;

/**
 * 对外稳定的业务错误码。HTTP 状态与错误码一一对应，避免把失败包装成 200。
 */
public enum ErrorCode {

    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "请求参数不合法"),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "请先登录"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "无权执行该操作"),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "预约不存在"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
    INVALID_STATE(HttpStatus.CONFLICT, "预约当前状态不允许该操作"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
