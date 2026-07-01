package com.mecash.account;
import com.mecash.repository.AccountRepository;
import com.mecash.service.account.AccountNumberGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

    @Mock
    private AccountRepository accountRepository;

    @Test
    void generatesA10DigitNumericString() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        AccountNumberGenerator generator = new AccountNumberGenerator(accountRepository);

        for (int i = 0; i < 1_000; i++) {
            String number = generator.generateUnique();
            assertThat(number).hasSize(10).containsOnlyDigits();
            assertThat(number.charAt(0)).isNotEqualTo('0'); // always exactly 10 significant digits
        }
    }

    @Test
    void retriesUntilItFindsAnUnusedNumber() {
        // First two candidates collide, third is free.
        when(accountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true, true, false);
        AccountNumberGenerator generator = new AccountNumberGenerator(accountRepository);

        String number = generator.generateUnique();

        assertThat(number).hasSize(10).containsOnlyDigits();
    }
}
