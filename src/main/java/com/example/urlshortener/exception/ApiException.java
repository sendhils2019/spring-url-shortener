package com.example.urlshortener.exception;

/** Validation / business-rule failure → 422. */
public class ApiException extends RuntimeException {
    public ApiException(String message) { super(message); }
}
