package com.mecash.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, client-facing errors. Each subclass carries the HTTP status
 * the {@code GlobalExceptionHandler} should translate it into.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
