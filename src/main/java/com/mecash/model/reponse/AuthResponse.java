package com.mecash.model.reponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String email
) {
    public static AuthResponse bearer(String token, long expiresInMs, String email) {
        return new AuthResponse(token, "Bearer", expiresInMs, email);
    }
}
