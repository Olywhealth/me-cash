package com.mecash.model.reponse;

import com.mecash.entity.Account;
import com.mecash.enums.Currency;
import java.math.BigDecimal;

public record BalanceResponse(
        String accountNumber,
        Currency currency,
        BigDecimal balance
) {
    public static BalanceResponse from(Account account) {
        return new BalanceResponse(
                account.getAccountNumber(),
                account.getCurrency(),
                account.getBalance());
    }
}
