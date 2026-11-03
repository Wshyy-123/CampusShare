package org.campus.campussharebackend.common;

import org.slf4j.MDC;

/**
 * 统一响应结构。code 为 OK 表示成功，其余为稳定业务错误码；requestId 用于定位一次调用。
 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static final String SUCCESS_CODE = "OK";

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(SUCCESS_CODE, message, data, currentRequestId());
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, currentRequestId());
    }

    private static String currentRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return requestId == null ? "unknown" : requestId;
    }
}
