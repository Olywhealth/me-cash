package com.mecash.transfer;
import com.mecash.entity.AccountUser;
import com.mecash.service.transfer.TransferService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mecash.entity.Account;
import com.mecash.repository.AccountRepository;
import com.mecash.enums.Currency;
import com.mecash.common.exception.BadRequestException;
import com.mecash.common.exception.ForbiddenException;
import com.mecash.common.exception.InsufficientFundsException;
import com.mecash.common.exception.NotFoundException;
import com.mecash.entity.Transaction;
import com.mecash.repository.TransactionRepository;
import com.mecash.enums.TransactionStatus;
import com.mecash.model.request.TransferRequest;
import com.mecash.repository.UserRepository;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;

    private AccountUser sender;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        sender = userRepository.save(AccountUser.builder()
                .email("sender@mecash.test")
                .password("hashed")
                .build());
    }

    private void account(String number, Currency currency, String balance, AccountUser owner) {
        accountRepository.save(Account.builder()
                .accountNumber(number)
                .currency(currency)
                .balance(new BigDecimal(balance))
                .owner(owner)
                .build());
    }

    @Test
    void transfersAcrossCurrenciesAToB() {
        account("1000000001", Currency.A, "500.00", sender);
        account("2000000002", Currency.B, "0.00", null);

        Transaction tx = transferService.transfer(sender.getEmail(),
                new TransferRequest("1000000001", "2000000002", new BigDecimal("100.00")));

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getSourceAmount()).isEqualByComparingTo("100.00");
        assertThat(tx.getDestinationAmount()).isEqualByComparingTo("134.55"); // 100 * 1.3455
        assertThat(tx.getExchangeRate()).isEqualByComparingTo("1.3455");

        assertThat(balanceOf("1000000001")).isEqualByComparingTo("400.00");
        assertThat(balanceOf("2000000002")).isEqualByComparingTo("134.55");
    }

    @Test
    void transfersAcrossCurrenciesBToA() {
        account("3000000003", Currency.B, "500.00", sender);
        account("4000000004", Currency.A, "0.00", null);

        Transaction tx = transferService.transfer(sender.getEmail(),
                new TransferRequest("3000000003", "4000000004", new BigDecimal("100.00")));

        // 100 / 1.3455 = 74.32 (HALF_EVEN)
        assertThat(tx.getDestinationAmount()).isEqualByComparingTo("74.32");
        assertThat(balanceOf("3000000003")).isEqualByComparingTo("400.00");
        assertThat(balanceOf("4000000004")).isEqualByComparingTo("74.32");
    }

    @Test
    void sameCurrencyTransferDoesNotConvert() {
        account("5000000005", Currency.A, "500.00", sender);
        account("6000000006", Currency.A, "10.00", null);

        Transaction tx = transferService.transfer(sender.getEmail(),
                new TransferRequest("5000000005", "6000000006", new BigDecimal("250.00")));

        assertThat(tx.getDestinationAmount()).isEqualByComparingTo("250.00");
        assertThat(tx.getExchangeRate()).isEqualByComparingTo("1");
        assertThat(balanceOf("5000000005")).isEqualByComparingTo("250.00");
        assertThat(balanceOf("6000000006")).isEqualByComparingTo("260.00");
    }

    @Test
    void rejectsTransferWhenBalanceIsInsufficientAndLeavesBalancesUnchanged() {
        account("7000000007", Currency.A, "50.00", sender);
        account("8000000008", Currency.B, "0.00", null);

        assertThatThrownBy(() -> transferService.transfer(sender.getEmail(),
                new TransferRequest("7000000007", "8000000008", new BigDecimal("100.00"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balanceOf("7000000007")).isEqualByComparingTo("50.00");
        assertThat(balanceOf("8000000008")).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void rejectsTransferToSameAccount() {
        account("9000000009", Currency.A, "100.00", sender);

        assertThatThrownBy(() -> transferService.transfer(sender.getEmail(),
                new TransferRequest("9000000009", "9000000009", new BigDecimal("10.00"))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTransferWhenDestinationDoesNotExist() {
        account("1100000011", Currency.A, "100.00", sender);

        assertThatThrownBy(() -> transferService.transfer(sender.getEmail(),
                new TransferRequest("1100000011", "9999999999", new BigDecimal("10.00"))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsTransferFromAnAccountTheCallerDoesNotOwn() {
        AccountUser other = userRepository.save(AccountUser.builder()
                .email("other@mecash.test").password("hashed").build());
        account("1200000012", Currency.A, "100.00", other);
        account("1300000013", Currency.B, "0.00", null);

        assertThatThrownBy(() -> transferService.transfer(sender.getEmail(),
                new TransferRequest("1200000012", "1300000013", new BigDecimal("10.00"))))
                .isInstanceOf(ForbiddenException.class);
    }

    /**
     * Proves the pessimistic lock serialises concurrent transfers: with a balance of 100 and
     * ten parallel transfers of 30, exactly three can succeed and the account never overdraws.
     */
    @Test
    void concurrentTransfersNeverOverdrawTheAccount() throws InterruptedException {
        account("1400000014", Currency.A, "100.00", sender);
        account("1500000015", Currency.A, "0.00", null);

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        ConcurrentLinkedQueue<Class<?>> failures = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    transferService.transfer(sender.getEmail(),
                            new TransferRequest("1400000014", "1500000015", new BigDecimal("30.00")));
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.add(e.getClass());
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(successes.get()).isEqualTo(3); // floor(100 / 30)
        assertThat(balanceOf("1400000014")).isEqualByComparingTo("10.00");
        assertThat(balanceOf("1500000015")).isEqualByComparingTo("90.00");
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }
}
