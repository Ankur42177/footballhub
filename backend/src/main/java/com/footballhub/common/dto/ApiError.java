package com.footballhub.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * The single error shape returned by every endpoint (section 9.9 of the plan).
 * {@code fieldErrors} is only populated for validation failures.
 */
public record ApiError(int status, String code, String message, Instant timestamp, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, Instant.now(), List.of());
    }

    public static ApiError validation(List<FieldError> fieldErrors) {
        return new ApiError(400, "VALIDATION_ERROR", "Please correct the highlighted fields.", Instant.now(),
                fieldErrors);
    }
}
