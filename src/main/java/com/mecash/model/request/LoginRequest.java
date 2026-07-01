package com.mecash.model.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials. The email address doubles as the username.
 */
public record LoginRequest(

        @NotBlank(message = "email is required")
        String email,

        @NotBlank(message = "password is required")
        String password
) {
}
