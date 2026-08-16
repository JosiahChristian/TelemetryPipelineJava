package com.telemetry.engine.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateTelemetryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTelemetry(
            DuplicateTelemetryException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "Duplicate telemetry",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(OutOfOrderTelemetryException.class)
    public ResponseEntity<Map<String, Object>> handleOutOfOrderTelemetry(
            OutOfOrderTelemetryException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "Out-of-order telemetry",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                message,
                request
        );
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
