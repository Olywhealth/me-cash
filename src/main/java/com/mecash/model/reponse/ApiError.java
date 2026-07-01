package com.mecash.model.reponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Uniform error envelope returned for every non-2xx response.
 *
 * @param timestamp when the error was produced
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param message   human-readable description
 * @param path      request path that produced the error
 * @param fieldErrors per-field validation messages (only present for validation failures)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path,
                              Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
