package com.mecash.service.transfer;

import com.mecash.entity.Account;
import com.mecash.repository.AccountRepository;
import com.mecash.service.account.AccountService;
import com.mecash.common.exception.BadRequestException;
import com.mecash.common.exception.ForbiddenException;
import com.mecash.common.exception.InsufficientFundsException;
import com.mecash.common.exception.NotFoundException;
import com.mecash.service.exchange.ConversionResult;
import com.mecash.service.exchange.ExchangeRateService;
import com.mecash.entity.Transaction;
import com.mecash.repository.TransactionRepository;
import com.mecash.enums.TransactionStatus;
import com.mecash.model.request.TransferRequest;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes cross-currency transfers atomically.
 *
 * <p>Both accounts are loaded under a pessimistic write lock (SELECT ... FOR UPDATE) acquired
 * in a deterministic order (sorted by account number) so concurrent transfers touching the
 * same accounts are serialised and cannot overdraw, and never deadlock. The whole method runs
 * in a single transaction: either both balances change and the transaction row is written, or
 * nothing is.
 */
@Service
public class TransferServiceImpl implements TransferService{

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;
    private final AccountService accountService;

    public TransferServiceImpl(AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               ExchangeRateService exchangeRateService,
                               AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateService = exchangeRateService;
        this.accountService = accountService;
    }

    @Transactional
    public Transaction transfer(String userEmail, TransferRequest request) {
        String sourceNumber = request.sourceAccountNumber();
        String destinationNumber = request.destinationAccountNumber();
        BigDecimal amount = request.amount();

        if (amount.signum() <= 0) {
            throw new BadRequestException("amount must be positive");
        }
        if (sourceNumber.equals(destinationNumber)) {
            throw new BadRequestException("Source and destination accounts must be different");
        }

        // Lock both rows in a stable order to prevent deadlocks under concurrency.
        Account first = lock(sourceNumber.compareTo(destinationNumber) <= 0 ? sourceNumber : destinationNumber);
        Account second = lock(first.getAccountNumber().equals(sourceNumber) ? destinationNumber : sourceNumber);
        Account source = first.getAccountNumber().equals(sourceNumber) ? first : second;
        Account destination = first.getAccountNumber().equals(destinationNumber) ? first : second;

        if (!accountService.isOwnedBy(source, userEmail)) {
            throw new ForbiddenException("You can only transfer from your own account");
        }

        ConversionResult conversion =
                exchangeRateService.convert(amount, source.getCurrency(), destination.getCurrency());

        if (source.getBalance().compareTo(conversion.sourceAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + sourceNumber);
        }

        source.setBalance(source.getBalance().subtract(conversion.sourceAmount()));
        destination.setBalance(destination.getBalance().add(conversion.convertedAmount()));

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .sourceAccountNumber(source.getAccountNumber())
                .destinationAccountNumber(destination.getAccountNumber())
                .sourceCurrency(source.getCurrency())
                .destinationCurrency(destination.getCurrency())
                .sourceAmount(conversion.sourceAmount())
                .destinationAmount(conversion.convertedAmount())
                .exchangeRate(conversion.rate())
                .status(TransactionStatus.SUCCESS)
                .build();

        // Balances are flushed via the managed entities; persist the audit record.
        return transactionRepository.save(transaction);
    }

    private Account lock(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNumber));
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
