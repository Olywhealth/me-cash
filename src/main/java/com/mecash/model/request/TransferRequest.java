package com.mecash.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request to move money between two accounts.
 *
 * <p>The {@code amount} is denominated in the <strong>source account's currency</strong>;
 * meCash converts it to the destination currency at today's rate before crediting.
 */
public record TransferRequest(

        @NotBlank(message = "sourceAccountNumber is required")
        String sourceAccountNumber,

        @NotBlank(message = "destinationAccountNumber is required")
        String destinationAccountNumber,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount
) {
}
