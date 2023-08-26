package com.skypro.simplebanking;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Init {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createInitUser(int index) {
        String userName = "User_" + index;
        User testUser = new User();
        testUser.setUsername(userName);
        String password = passwordEncoder.encode("initPassword");
        testUser.setPassword(password);
        User savedUser = userRepository.save(testUser);
        return savedUser;
    }

    public Account createNewAccount(User user, AccountCurrency currency) {
        List<Account> userAccounts = new ArrayList<>();
        user.setAccounts(userAccounts);
        Account account = new Account();
        account.setAccountCurrency(currency);
        account.setAmount(100L);
        account.setUser(user);
        userAccounts.add(account);
        user.setAccounts(userAccounts);
        Account returnAccount = accountRepository.save(account);
        return returnAccount;
    }
    public BankingUserDetails getAuthUser(long id) {
        User user = userRepository.findById(id).orElseThrow();
        return new BankingUserDetails(user.getId(), user.getUsername(), user.getPassword(), false);
    }
    public JSONObject createTransferJson(Account sourceAccount, Account destinationAccount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fromAccountId", sourceAccount.getId());
        jsonObject.put("toUserId", destinationAccount.getUser().getId());
        jsonObject.put("toAccountId", destinationAccount.getId());
        jsonObject.put("amount", sourceAccount.getAmount());
        return jsonObject;
    }
}
