package com.mecash.controller;
import com.mecash.service.account.AccountService;

import com.mecash.model.reponse.AccountResponse;
import com.mecash.model.reponse.BalanceResponse;
import com.mecash.model.reponse.PagedResponse;
import com.mecash.entity.Transaction;
import com.mecash.repository.TransactionRepository;
import com.mecash.model.reponse.TransactionResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public AccountController(AccountService accountService,
                             TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    /** Lists the accounts owned by the authenticated user. */
    @GetMapping("/me")
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal String email) {
        return accountService.findAccountsOwnedBy(email).stream()
                .map(AccountResponse::from)
                .toList();
    }

    /** Returns the balance of one of the caller's accounts. */
    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse balance(@AuthenticationPrincipal String email,
                                   @PathVariable String accountNumber) {
        return BalanceResponse.from(accountService.getOwnedAccount(accountNumber, email));
    }

    /** Returns the transaction history (money in and out) for one of the caller's accounts. */
    @GetMapping("/{accountNumber}/transactions")
    public PagedResponse<TransactionResponse> transactions(
            @AuthenticationPrincipal String email,
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Ownership check before exposing any history.
        accountService.getOwnedAccount(accountNumber, email);

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Transaction> history = transactionRepository.findHistoryForAccount(accountNumber, pageable);
        List<TransactionResponse> content = history.getContent().stream()
                .map(tx -> TransactionResponse.forAccount(tx, accountNumber))
                .toList();
        return PagedResponse.from(history, content);
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
