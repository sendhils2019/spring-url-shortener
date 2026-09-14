package com.example.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", false);
        payload.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(payload);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", false);
        FieldError fieldError = ex.getBindingResult().getFieldError();
        payload.put("error", fieldError == null ? "Validation failed" : fieldError.getDefaultMessage());
        return ResponseEntity.badRequest().body(payload);
    }
}
