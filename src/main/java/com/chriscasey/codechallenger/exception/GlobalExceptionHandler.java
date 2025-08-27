package com.chriscasey.codechallenger.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import com.chriscasey.codechallenger.exception.RefreshTokenException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleRefreshToken(RefreshTokenException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req, null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req, null);
    }

    // Rate limiting for challenge submissions
    @ExceptionHandler(SubmissionRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleSubmissionRateLimit(SubmissionRateLimitException ex, HttpServletRequest req) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Rate Limited", ex.getMessage(), req, null);
    }

    // Business-rule conflict (e.g., submitting a non-pending challenge)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req, null);
    }

    // LLM parsing issues treated as upstream failure
    @ExceptionHandler(LlmParseException.class)
    public ResponseEntity<ErrorResponse> handleLlmParse(LlmParseException ex, HttpServletRequest req) {
        // Avoid echoing raw model payload by default; message is enough
        return build(HttpStatus.BAD_GATEWAY, "Bad Gateway", ex.getMessage(), req, null);
    }

    // Optimistic locking -> 409
    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", "The resource was modified concurrently. Please retry.", req, null);
    }

    // @Valid on @RequestBody failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldErrorItem> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldErrorItem(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed", req, details);
    }

    // @Validated on params (e.g., @RequestParam, @PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldErrorItem> details = ex.getConstraintViolations().stream()
                .map(this::toItem)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed", req, details);
    }

    // Malformed JSON, wrong types in body, etc.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body", req, null);
    }

    // Fallback: unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, HttpServletRequest req,
                                                List<ErrorResponse.FieldErrorItem> details) {
        ErrorResponse body = ErrorResponse.of(status.value(), error, message, pathOf(req), details);
        return new ResponseEntity<>(body, new HttpHeaders(), status);
    }

    private String pathOf(HttpServletRequest req) {
        return req != null ? req.getRequestURI() : null;
    }

    private ErrorResponse.FieldErrorItem toItem(ConstraintViolation<?> v) {
        String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : null;
        String msg = v.getMessage() != null ? v.getMessage() : "Invalid value";
        return new ErrorResponse.FieldErrorItem(field, msg);
    }
}