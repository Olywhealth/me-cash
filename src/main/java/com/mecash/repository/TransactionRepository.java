package com.mecash.repository;

import com.mecash.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Full history for an account: rows where it was either the source (money out) or the
     * destination (money in), newest first.
     */
    @Query("""
            select t from Transaction t
            where t.sourceAccountNumber = :accountNumber
               or t.destinationAccountNumber = :accountNumber
            order by t.createdAt desc, t.id desc
            """)
    Page<Transaction> findHistoryForAccount(@Param("accountNumber") String accountNumber, Pageable pageable);
}
