package com.mecash.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecash.entity.Account;
import com.mecash.repository.AccountRepository;
import com.mecash.enums.Currency;
import com.mecash.repository.TransactionRepository;
import com.mecash.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end HTTP tests covering the full happy path (sign up -> log in -> transfer ->
 * balance -> history) plus the main guard rails, exercised through the real security filter
 * chain and JSON (de)serialisation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndTransferApiIntegrationTest {

    private static final String DEST_A = "1234567890";
    private static final String DEST_B = "6574839201";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        // Re-seed the two destination accounts from the brief.
        seed(DEST_A, Currency.A);
        seed(DEST_B, Currency.B);
    }

    private void seed(String number, Currency currency) {
        accountRepository.save(Account.builder()
                .accountNumber(number)
                .currency(currency)
                .balance(new BigDecimal("100000.00"))
                .owner(null)
                .build());
    }

    @Test
    void fullJourney_signupLoginTransferBalanceHistory() throws Exception {
        // 1. Sign up -> an account is auto-created with a random number, currency and opening balance.
        MvcResult signup = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "alice@example.com", "password", "supersecret"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.account.balance").value(1000.00))
                .andReturn();

        JsonNode account = read(signup).get("account");
        String accountNumber = account.get("accountNumber").asText();
        assertThat(accountNumber).hasSize(10).containsOnlyDigits();
        assertThat(account.get("currency").asText()).isIn("A", "B");

        // 2. Log in -> bearer token.
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "alice@example.com", "password", "supersecret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        String token = read(login).get("accessToken").asText();
        assertThat(token).isNotBlank();

        // 3. Transfer 100 (in the sender's currency) to a pre-existing account.
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceAccountNumber", accountNumber,
                                "destinationAccountNumber", DEST_A,
                                "amount", "100.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction").value("DEBIT"))
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.sourceAmount").value(100.00));

        // 4. Balance reflects the debit (amount is in the source currency -> 1000 - 100).
        mockMvc.perform(get("/api/v1/accounts/{n}/balance", accountNumber)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(900.00));

        // 5. History shows the outgoing transfer.
        mockMvc.perform(get("/api/v1/accounts/{n}/transactions", accountNumber)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].destinationAccountNumber").value(DEST_A));
    }

    @Test
    void transferWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceAccountNumber", DEST_B,
                                "destinationAccountNumber", DEST_A,
                                "amount", "10.00"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupWithDuplicateEmailIsConflict() throws Exception {
        String body = json(Map.of("email", "bob@example.com", "password", "supersecret"));
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "carol@example.com", "password", "supersecret"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "carol@example.com", "password", "wrongpass1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupWithInvalidEmailIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", "supersecret"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void cannotReadAnotherUsersAccount() throws Exception {
        // Alice signs up and owns an account.
        MvcResult signup = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "alice2@example.com", "password", "supersecret"))))
                .andExpect(status().isCreated()).andReturn();
        String aliceAccount = read(signup).get("account").get("accountNumber").asText();

        // Mallory signs up and logs in.
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "mallory@example.com", "password", "supersecret"))))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "mallory@example.com", "password", "supersecret"))))
                .andReturn();
        String malloryToken = read(login).get("accessToken").asText();

        // Mallory cannot see Alice's balance.
        mockMvc.perform(get("/api/v1/accounts/{n}/balance", aliceAccount)
                        .header("Authorization", "Bearer " + malloryToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferBetweenTwoRealUsersShowsAsCreditForTheRecipient() throws Exception {
        // Two real users sign up; both get a funded account.
        JsonNode senderAccount = read(signup("sender@example.com")).get("account");
        JsonNode recipientAccount = read(signup("recipient@example.com")).get("account");
        String senderNumber = senderAccount.get("accountNumber").asText();
        String recipientNumber = recipientAccount.get("accountNumber").asText();
        String senderToken = login("sender@example.com");
        String recipientToken = login("recipient@example.com");

        // Sender transfers 100 (in their currency) to the recipient.
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceAccountNumber", senderNumber,
                                "destinationAccountNumber", recipientNumber,
                                "amount", "100.00"))))
                .andExpect(status().isCreated());

        // The recipient sees the inbound transfer as a CREDIT in their own history.
        mockMvc.perform(get("/api/v1/accounts/{n}/transactions", recipientNumber)
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].sourceAccountNumber").value(senderNumber))
                .andExpect(jsonPath("$.content[0].destinationAccountNumber").value(recipientNumber));

        // And the recipient's balance increased by the converted amount.
        BigDecimal recipientOpening = recipientAccount.get("balance").decimalValue();
        Currency senderCurrency = Currency.valueOf(senderAccount.get("currency").asText());
        Currency recipientCurrency = Currency.valueOf(recipientAccount.get("currency").asText());
        BigDecimal expectedCredit = convert(new BigDecimal("100.00"), senderCurrency, recipientCurrency);

        MvcResult balance = mockMvc.perform(get("/api/v1/accounts/{n}/balance", recipientNumber)
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal recipientBalance = read(balance).get("balance").decimalValue();
        assertThat(recipientBalance).isEqualByComparingTo(recipientOpening.add(expectedCredit));
    }

    /** Mirrors ExchangeRateServiceImpl's policy so the expected credit is computed, not hard-coded. */
    private BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        BigDecimal rate = new BigDecimal("1.3455");
        if (from == to) {
            return amount.setScale(2, java.math.RoundingMode.HALF_EVEN);
        }
        if (from == Currency.A) {
            return amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_EVEN);
        }
        return amount.divide(rate, java.math.MathContext.DECIMAL64).setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    private MvcResult signup(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "supersecret"))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "supersecret"))))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).get("accessToken").asText();
    }

    private String json(Map<String, String> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
