package com.skypro.simplebanking.controllerTest;

import com.skypro.simplebanking.Init;
import com.skypro.simplebanking.dto.BankingUserDetails;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class UserControllerTest {
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
    //@Transactional
    void createUserByAdminAndGetResponse_isOk(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "testUser");
        jsonObject.put("password", "testPassword");

        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toString()))
                .andExpect(status().isOk());
    }


    @Test
    void createUserByUserAndGetResponse_isForbidden() throws Exception {
        int userOrderNumber = 1;
        init.createInitUser(userOrderNumber);
        List<User> users = userRepository.findAll();
        User user = users.get(users.size() - 1);
        BankingUserDetails authUser = init.getAuthUser(user.getId());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "testUser");
        jsonObject.put("password", "testPassword");

        mockMvc.perform(post("/user/").with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenGetAllUsersByAuthorizedUser_thenIsNotEmptyJsonArray() throws Exception {
        int userOrderNumber = 1;
        User user_1 = init.createInitUser(userOrderNumber);
        String base64Encoded = Base64Utils.encodeToString((user_1.getUsername() + ":" + "initPassword")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/user/list/")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded))
                .andExpect(status().isOk())
                .andExpectAll(jsonPath("$").isArray(), jsonPath("$").isNotEmpty());
    }
    @Test
    //@Transactional
    void whenGetAllUsersByAdmin_thenStatusIsForbidden(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        int userOrderNumber = 1;
        User user_1 = init.createInitUser(userOrderNumber);

        mockMvc.perform(get("/user/list")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());

        String base64Encoded = Base64Utils.encodeToString(("admin" + ":" + "X-SECURITY-ADMIN-KEY")
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/user/list/")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded))
                .andExpect(status().isUnauthorized());
    }
    @Test
    //@Transactional
    void whenGetProfileByAuthorizedUser_thenStatusIsOk() throws Exception {
        int userOrderNumber = 1;
        User user_1 = init.createInitUser(userOrderNumber);
        init.createNewAccount(user_1, AccountCurrency.RUB);
        BankingUserDetails authUser = init.getAuthUser(user_1.getId());
        mockMvc.perform(get("/user/me").with(user(authUser)))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(user_1.getId()),
                        jsonPath("$.username").value(user_1.getUsername())
                );
    }
    @Test
    //@Transactional
    void whenGetProfileByNotAuthorizedUser_thenStatusIsUnauthorized() throws Exception {
        int userOrderNumber = 1;
        User user_1 = init.createInitUser(userOrderNumber);
        init.createNewAccount(user_1, AccountCurrency.RUB);
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetProfileByAdmin_thenStatusIsForbidden(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
    }
}
