package com.mecash.service.account;
import com.mecash.entity.Account;
import com.mecash.entity.AccountUser;
import com.mecash.repository.AccountRepository;

import com.mecash.enums.Currency;
import com.mecash.common.exception.ForbiddenException;
import com.mecash.common.exception.NotFoundException;
import com.mecash.config.MecashProperties;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService{

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final BigDecimal openingBalance;
    private final SecureRandom random = new SecureRandom();

    public AccountServiceImpl(AccountRepository accountRepository,
                              AccountNumberGenerator accountNumberGenerator,
                              MecashProperties properties) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.openingBalance = properties.getAccount().getOpeningBalance();
    }

    /**
     * Opens an account for a newly registered accountUser with a randomly assigned currency and the
     * configured opening balance, so the accountUser can immediately transfer funds.
     */
    @Transactional
    public Account openAccountForNewUser(AccountUser accountUser) {
        Currency currency = random.nextBoolean() ? Currency.A : Currency.B;
        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.generateUnique())
                .currency(currency)
                .balance(openingBalance)
                .owner(accountUser)
                .build();
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> findAccountsOwnedBy(String email) {
        return accountRepository.findByOwnerEmailIgnoreCase(email);
    }

    /**
     * Loads an account and asserts the given user owns it. Returns 404 if it does not exist
     * and 403 if it belongs to someone else (so we never reveal another user's balance).
     */
    @Transactional(readOnly = true)
    public Account getOwnedAccount(String accountNumber, String email) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNumber));
        if (!isOwnedBy(account, email)) {
            throw new ForbiddenException("You do not have access to this account");
        }
        return account;
    }

    public boolean isOwnedBy(Account account, String email) {
        return account.getOwner() != null
                && account.getOwner().getEmail().equalsIgnoreCase(email);
    }
}
