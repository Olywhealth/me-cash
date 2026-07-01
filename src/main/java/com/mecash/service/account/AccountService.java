package com.mecash.service.account;

import com.mecash.entity.Account;
import com.mecash.entity.AccountUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AccountService {
    @Transactional
    Account openAccountForNewUser(AccountUser accountUser);

    @Transactional(readOnly = true)
    List<Account> findAccountsOwnedBy(String email);

    @Transactional(readOnly = true)
    Account getOwnedAccount(String accountNumber, String email);

    boolean isOwnedBy(Account account, String email);
}
