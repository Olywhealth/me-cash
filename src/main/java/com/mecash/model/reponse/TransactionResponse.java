package com.mecash.model.reponse;

import com.mecash.enums.Currency;
import com.mecash.entity.Transaction;
import com.mecash.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A transaction as seen from the perspective of a particular account.
 *
 * @param direction DEBIT when the viewing account sent the money, CREDIT when it received it
 */
public record TransactionResponse(
        String reference,
        Direction direction,
        String sourceAccountNumber,
        String destinationAccountNumber,
        Currency sourceCurrency,
        Currency destinationCurrency,
        BigDecimal sourceAmount,
        BigDecimal destinationAmount,
        BigDecimal exchangeRate,
        TransactionStatus status,
        Instant createdAt
) {
    public enum Direction {
        DEBIT,
        CREDIT
    }

    public static TransactionResponse forAccount(Transaction tx, String accountNumber) {
        Direction direction = tx.getSourceAccountNumber().equals(accountNumber)
                ? Direction.DEBIT
                : Direction.CREDIT;
        return new TransactionResponse(
                tx.getReference(),
                direction,
                tx.getSourceAccountNumber(),
                tx.getDestinationAccountNumber(),
                tx.getSourceCurrency(),
                tx.getDestinationCurrency(),
                tx.getSourceAmount(),
                tx.getDestinationAmount(),
                tx.getExchangeRate(),
                tx.getStatus(),
                tx.getCreatedAt());
    }
}
