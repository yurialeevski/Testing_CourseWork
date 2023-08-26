package com.skypro.simplebanking.controllerTest;

import com.skypro.simplebanking.Init;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:tc:postgresql:15.2-alpine:///db"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private Init init;

    @Test
    void whenAccessToAccountByAdmin_thenStatusIsForbidden(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        mockMvc.perform(get("/account/")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenAccessToAccountByAnotherUser_thenStatusIsUnauthorized() throws Exception {
        User user1 = init.createInitUser(1);
        Account sourceAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        User user2 = init.createInitUser(2);
        Account destinationAccount = init.createNewAccount(user2, AccountCurrency.RUB);
        JSONObject transferJson = init.createTransferJson(sourceAccount, destinationAccount);
        String base64Encoded = Base64Utils.encodeToString(("userX" + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(post("/account/", sourceAccount.getUser().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson.toString()))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void whenAuthorizedAccessToAccount_thenStatusIsOk() throws Exception {
        User user1 = init.createInitUser(1);
        Account user1RubAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        String base64Encoded = Base64Utils.encodeToString((user1.getUsername() + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/account/" + user1RubAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded))
                .andExpect(status().isOk());
    }

    @Test
    void whenGetUserAccountByUser_thenStatusIsOk() throws Exception {
        int userOrderNumber = 1;
        User user1 = init.createInitUser(userOrderNumber);
        Account user1RubAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        //init.createNewAccount(user_1, AccountCurrency.RUB);
        //Collection<Account> userAccounts = user_1.getAccounts();
        //List<Account> accountList = userAccounts.stream().toList();
        //Account account = accountList.get(0);
        BankingUserDetails authUser = init.getAuthUser(user1.getId());
        mockMvc.perform(get("/account/{id}", user1RubAccount.getId()).with(user(authUser)))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(user1RubAccount.getId()),
                        jsonPath("$.currency").value(user1RubAccount.getAccountCurrency().name()),
                        jsonPath("$.amount").value(user1RubAccount.getAmount()));
    }

    @Test
    void whenDepositToAccount_thenStatusIsOkAndAssertEqualsIsCorrect() throws Exception {
        int userOrderNumber = 1;
        User user1 = init.createInitUser(userOrderNumber);
        Account user1RubAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        long initialAmount = user1RubAccount.getAmount();
        long resultAmount = initialAmount * 2;
        JSONObject depositJson = new JSONObject();
        depositJson.put("amount", initialAmount);
        String base64Encoded = Base64Utils.encodeToString((user1.getUsername() + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(post("/account/deposit/{id}", user1RubAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1RubAccount.getId()))
                .andExpect(jsonPath("$.currency").value(user1RubAccount.getAccountCurrency().name()))
                .andExpect(jsonPath("$.amount").value(resultAmount));

        long currentAmount = accountRepository.findById(user1RubAccount.getId()).orElseThrow().getAmount();
        assertEquals(resultAmount, currentAmount);
    }
    @Test
    void whenWithdrawFromAccount_thenStatusIsOkAndAssertEqualsIsCorrect() throws Exception {
        int userOrderNumber = 1;
        User user1 = init.createInitUser(userOrderNumber);
        Account user1RubAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        long initialAmount = user1RubAccount.getAmount();
        long withdrawAmount = initialAmount / 2;
        JSONObject withdrawJson = new JSONObject();
        withdrawJson.put("amount", withdrawAmount);
        String base64Encoded = Base64Utils.encodeToString((user1.getUsername() + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(post("/account/withdraw/{id}", user1RubAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawJson.toString()))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(user1RubAccount.getId()),
                        jsonPath("$.currency").value(user1RubAccount.getAccountCurrency().name()),
                        jsonPath("$.amount").value(withdrawAmount));
        long currentAmount = accountRepository.findById(user1RubAccount.getId()).orElseThrow().getAmount();
        assertEquals(withdrawAmount, currentAmount);
    }
}
