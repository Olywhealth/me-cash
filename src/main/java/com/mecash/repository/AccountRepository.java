package com.mecash.repository;

import com.mecash.entity.Account;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByOwnerEmailIgnoreCase(String email);

    /**
     * Loads an account while taking a row-level write lock (SELECT ... FOR UPDATE).
     * Used by the transfer flow so concurrent transfers touching the same account are
     * serialised and cannot overdraw it. Callers must acquire locks in a deterministic
     * order (sorted by account number) to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
