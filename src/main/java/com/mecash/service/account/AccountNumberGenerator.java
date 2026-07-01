package com.mecash.service.account;
import com.mecash.repository.AccountRepository;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates unique 10-digit account numbers. Uniqueness is checked against persisted
 * accounts, which includes the two seeded numbers from the brief, so a freshly created
 * user can never collide with {@code 1234567890} or {@code 6574839201}.
 */
@Component
public class AccountNumberGenerator {

    /** First digit is 1-9 so the number is always exactly 10 digits when rendered. */
    private static final long MIN = 1_000_000_000L;
    private static final long BOUND = 9_000_000_000L; // 10^10 - 10^9
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = Long.toString(MIN + (long) (random.nextDouble() * BOUND));
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        // Astronomically unlikely with a 9-billion key space and few accounts.
        throw new IllegalStateException("Could not generate a unique account number");
    }
}
