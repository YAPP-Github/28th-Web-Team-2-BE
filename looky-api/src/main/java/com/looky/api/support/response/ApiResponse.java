package com.looky.api.support.response;

public record ApiResponse<T>(
        String status,
        String message,
        T payload
) {
    public static <T> ApiResponse<T> success(String message, T payload) {
        return new ApiResponse<>("success", message, payload);
    }

    public static <T> ApiResponse<T> fail(String message, T payload) {
        return new ApiResponse<>("fail", message, payload);
    }
}
