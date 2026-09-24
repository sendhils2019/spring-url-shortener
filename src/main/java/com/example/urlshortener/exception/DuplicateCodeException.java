package com.example.urlshortener.exception;

/** Thrown by LinkRepository when a code already exists (PK collision). */
public class DuplicateCodeException extends RuntimeException {
    public DuplicateCodeException(String code) { super(code); }
}
