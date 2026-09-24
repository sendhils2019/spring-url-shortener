package com.example.urlshortener.repository;

/** Plain domain record for a stored link. createdAt is epoch seconds. */
public record Link(String code, String url, double createdAt, boolean isCustom) {}
