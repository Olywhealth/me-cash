package com.mecash.entity;

import com.mecash.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 10-digit account number. Stored as a String so leading zeros are preserved and
     * it is never treated as an arithmetic value.
     */
    @Column(name = "account_number", nullable = false, unique = true, length = 10)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Currency currency;

    /** Always handled as BigDecimal. precision/scale fixed so JPA maps NUMERIC(19,2). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     * Owning user. Nullable on purpose: the two pre-existing accounts from the brief
     * (1234567890 / 6574839201) are seeded without an owner and serve as transfer targets.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AccountUser owner;

    /** Optimistic-lock guard, complements the pessimistic lock taken during transfers. */
    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
