package com.mecash.model.reponse;

/**
 * Returned on successful sign-up: the new user's email and the account that was
 * automatically opened for them (number, randomly assigned currency, opening balance).
 */
public record SignupResponse(
        String email,
        AccountResponse account
) {
}
