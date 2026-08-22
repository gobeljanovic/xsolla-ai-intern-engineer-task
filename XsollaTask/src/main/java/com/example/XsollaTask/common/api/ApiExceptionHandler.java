package com.example.XsollaTask.common.api;

import com.example.XsollaTask.review.diff.InvalidDiffException;
import com.example.XsollaTask.review.job.ReviewJobNotFoundException;
import com.example.XsollaTask.review.submission.IdempotencyConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class ApiExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidDiffException.class)
    public ResponseEntity<ApiErrorResponse> invalidDiff(
            InvalidDiffException exception
    ) {
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "invalid_diff",
                exception.getMessage()
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            InvalidJsonException.class
    })
    public ResponseEntity<ApiErrorResponse> invalidJson() {
        return response(
                HttpStatus.BAD_REQUEST,
                "invalid_json",
                "Request body contains invalid JSON"
        );
    }

    @ExceptionHandler(
            ReviewJobNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse> notFound(
            ReviewJobNotFoundException exception
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "not_found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> internal(
            Exception exception
    ) {
        LOGGER.error(
                "Unhandled request failure",
                exception
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal",
                "Internal server error"
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(
                        code,
                        message
                ));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> idempotencyConflict(
            IdempotencyConflictException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "idempotency_conflict",
                exception.getMessage()
        );
    }
}