package com.example.XsollaTask.common.api;

public record ApiErrorResponse(ApiError error) {

    public static ApiErrorResponse unauthorized(String message) {
        return new ApiErrorResponse(
                new ApiError("unauthorized", message)
        );
    }
}
