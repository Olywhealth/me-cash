package com.mecash.model.reponse;

import com.mecash.entity.Account;
import com.mecash.enums.Currency;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountNumber,
        Currency currency,
        BigDecimal balance,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt());
    }
}
