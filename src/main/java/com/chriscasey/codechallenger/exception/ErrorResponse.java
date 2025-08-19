package com.chriscasey.codechallenger.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> details
) {
    public static ErrorResponse of(int status, String error, String message, String path, List<FieldErrorItem> details) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, path, details);
    }

    public record FieldErrorItem(String field, String message) {}
}