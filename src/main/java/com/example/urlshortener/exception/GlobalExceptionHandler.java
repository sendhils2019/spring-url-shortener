package com.example.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException exc) {
        return ResponseEntity.status(422).body(Map.of("detail", exc.getMessage()));
    }

    @ExceptionHandler(DuplicateCodeException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateCodeException exc) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("detail", "custom_alias '" + exc.getMessage() + "' is already taken"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException exc) {
        return ResponseEntity.status(422).body(Map.of("detail", "request body is invalid or contains unknown fields"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exc) {
        String message = exc.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("invalid request");
        return ResponseEntity.status(422).body(Map.of("detail", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exc) {
        return ResponseEntity.status(exc.getStatusCode())
                .body(Map.of("detail", exc.getReason() != null ? exc.getReason() : exc.getMessage()));
    }
}
