package com.mecash.config;

import com.mecash.entity.Account;
import com.mecash.repository.AccountRepository;
import com.mecash.enums.Currency;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds the two pre-existing accounts named in the brief. These have no owner, so they cannot
 * log in or initiate transfers, but they are valid destinations for testing cross-currency
 * transfers. Idempotent: only inserts what is missing, so it is safe to run on every startup.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    static final String SEED_ACCOUNT_A = "1234567890";
    static final String SEED_ACCOUNT_B = "6574839201";
    static final BigDecimal SEED_BALANCE = new BigDecimal("100000.00");

    @Bean
    ApplicationRunner seedExistingAccounts(AccountRepository accountRepository) {
        return args -> {
            seedIfAbsent(accountRepository, SEED_ACCOUNT_A, Currency.A);
            seedIfAbsent(accountRepository, SEED_ACCOUNT_B, Currency.B);
        };
    }

    private void seedIfAbsent(AccountRepository repository, String accountNumber, Currency currency) {
        if (repository.existsByAccountNumber(accountNumber)) {
            return;
        }
        repository.save(Account.builder()
                .accountNumber(accountNumber)
                .currency(currency)
                .balance(SEED_BALANCE)
                .owner(null)
                .build());
        log.info("Seeded pre-existing account {} ({})", accountNumber, currency);
    }
}
