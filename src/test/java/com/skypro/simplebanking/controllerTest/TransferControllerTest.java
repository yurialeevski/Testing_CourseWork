package com.skypro.simplebanking.controllerTest;

import com.skypro.simplebanking.Init;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:tc:postgresql:15.2-alpine:///db"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Init init;

    @Test
    void whenTransferByAdmin_thenStatusIsForbidden(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        mockMvc.perform(get("/transfer")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
    }
    @Test
    void whenTransferFromNotValidUserAccount_thenStatusIsNotFound(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        User user1 = init.createInitUser(1);
        Account sourceAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        User user2 = init.createInitUser(2);
        Account destinationAccount = init.createNewAccount(user2, AccountCurrency.RUB);
        JSONObject transferJson = init.createTransferJson(sourceAccount, destinationAccount);
        BankingUserDetails authUser1 = init.getAuthUser(destinationAccount.getUser().getId());
        mockMvc.perform(post("/transfer").with(user(authUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson.toString()))
                .andExpect(status().isNotFound());
    }
    @Test
    void whenTransferByUnauthorizedUser_thenStatusIsUnauthorized() throws Exception {
        User user1 = init.createInitUser(1);
        Account sourceAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        User user2 = init.createInitUser(2);
        Account destinationAccount = init.createNewAccount(user2, AccountCurrency.RUB);
        JSONObject transferJson = init.createTransferJson(sourceAccount, destinationAccount);
        String base64Encoded = Base64Utils.encodeToString(("userX" + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(post("/transfer", sourceAccount.getUser().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTransferFromNotValidCurrencyAccount_thenStatusIsBadRequest() throws Exception {
        User user1 = init.createInitUser(1);
        Account sourceAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        User user2 = init.createInitUser(2);
        Account destinationAccount = init.createNewAccount(user2, AccountCurrency.EUR);
        JSONObject transferJson = init.createTransferJson(sourceAccount, destinationAccount);
        String base64Encoded = Base64Utils.encodeToString((user1.getUsername() + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(post("/transfer", sourceAccount.getUser().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Account currencies should be same"));
    }

    @Test
    void whenTransferWithCorrectData_thenStatusIsOk() throws Exception {
        User user1 = init.createInitUser(1);
        Account sourceAccount = init.createNewAccount(user1, AccountCurrency.RUB);
        User user2 = init.createInitUser(2);
        Account destinationAccount = init.createNewAccount(user2, AccountCurrency.RUB);
        long destinationAccountResultAmount = destinationAccount.getAmount() + sourceAccount.getAmount();
        JSONObject transferJson = init.createTransferJson(sourceAccount, destinationAccount);
        String base64Encoded = Base64Utils.encodeToString((user1.getUsername() + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(post("/transfer", sourceAccount.getUser().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson.toString()))
                .andExpect(status().isOk());
        long currentDestinationAccountAmount = accountRepository.findById(user2.getId()).orElseThrow().getAmount();
        assertEquals(destinationAccountResultAmount, currentDestinationAccountAmount);
    }
}
