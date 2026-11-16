package org.campus.campussharebackend.common;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("OK", message, data, RequestIdContext.get());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, RequestIdContext.get());
    }
}
