package com.example.XsollaTask.common.api;

public record ApiErrorResponse(ApiError error) {

    public static ApiErrorResponse of(
            String code,
            String message
    ) {
        return new ApiErrorResponse(
                new ApiError(code, message)
        );
    }

    public static ApiErrorResponse unauthorized(
            String message
    ) {
        return of("unauthorized", message);
    }
}
