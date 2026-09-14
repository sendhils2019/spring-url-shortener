package com.example.urlshortener.exception;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
