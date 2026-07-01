package com.mecash.entity;

import com.mecash.enums.Currency;
import com.mecash.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Immutable record of a single transfer. Account numbers are denormalised onto the row
 * (rather than FK joins) so history reads are cheap and remain readable even for the
 * ownerless seeded accounts. All money fields are auditable: we persist the source amount,
 * the converted destination amount, and the exact rate applied.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_source", columnList = "source_account_number"),
        @Index(name = "idx_tx_destination", columnList = "destination_account_number")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, client-facing identifier (the DB id is never exposed). */
    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @Column(name = "source_account_number", nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false, length = 10)
    private String destinationAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Currency sourceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Currency destinationCurrency;

    /** Amount debited from the source, in the source currency. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal sourceAmount;

    /** Amount credited to the destination, in the destination currency. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal destinationAmount;

    /** Effective source->destination rate applied (1 for same-currency transfers). */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
