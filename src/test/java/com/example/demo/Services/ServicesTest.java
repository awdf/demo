package com.example.demo.Services;

import com.example.demo.exceptions.InsufficientFundsException;
import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.exceptions.UnknownCurrencyException;
import com.example.demo.models.User;
import com.example.demo.services.AccountManagmentService;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.services.UserManagmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class ServicesTest {
    private static final Logger log = LoggerFactory.getLogger(ServicesTest.class);

    private final static long user_id = 1L;
    private final static String GBP = "GBP";
    private final static String EUR = "EUR";
    private final static String USD = "USD";

    @Autowired
    UserManagmentService users;

    @Autowired
    AccountManagmentService accounts;

    @Autowired
    CurrencyManagmentService currency;

    @Test
    @DisplayName("User Check 😱")
    public void userCheck() throws Exception {
        assertEquals(users.getUserById(user_id).getId(), user_id);
    }

    @Test
    @DisplayName("Service performance test")
    public void performanceCheck() throws Exception {
        assertNotNull(users);
        assertNotNull(accounts);
        assertNotNull(currency);

        User user = users.createUser(currency.getAllCurrencies());

        long operations = 0;
        Instant start = Instant.now();
        do {
            accounts.deposit(user.getId(), 100, GBP);
            accounts.withdraw(user.getId(), 100, GBP);
            accounts.balance(user.getId());
            operations+=3;
        } while (Duration.between(start, Instant.now()).toMillis() < 1000);
        log.info("Service performance {} (ops): ", operations);

        users.removeUser(user.getId());
    }

    @Test
    @DisplayName("Service exceptions test")
    public void exceptionsCheck() {
        assertThrows(NoSuchUserException.class, () -> accounts.deposit(Long.MAX_VALUE, 1, GBP));
        assertThrows(UnknownCurrencyException.class, () -> accounts.deposit(user_id, 1, "UAH"));

        assertThrows(NoSuchUserException.class, () -> accounts.withdraw(Long.MAX_VALUE, 1, GBP));
        assertThrows(UnknownCurrencyException.class, () -> accounts.withdraw(user_id, 1, "UAH"));
        assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, Long.MAX_VALUE, GBP));

        assertThrows(NoSuchUserException.class, () -> accounts.balance(Long.MAX_VALUE));
    }

    @TestFactory
    @DisplayName("Sequence of actions test")
    Collection<DynamicTest> accountsActions() throws NoSuchUserException {

        assertNotNull(users);
        assertNotNull(accounts);

        User user = users.getUserById(user_id);

        return Arrays.asList(
            dynamicTest("1.Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("2.Make a deposit of USD 100",
                ()-> accounts.deposit(user_id, 100, USD)),



            dynamicTest("3.Check that all balances are correct",
                ()-> assertAll("userBalancesCheck1",
                    ()-> assertTrue(balanceCheck(user, 0, GBP)),
                    ()-> assertTrue(balanceCheck(user, 0, EUR)),
                    ()-> assertTrue(balanceCheck(user, 100, USD))
                )),



            dynamicTest("4. Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("5. Make a deposit of EUR 100",
                ()-> accounts.deposit(user_id, 100, EUR)),



            dynamicTest("6. Check that all balances are correct",
                ()-> assertAll("userBalancesCheck2",
                        ()-> assertTrue(balanceCheck(user, 0, GBP)),
                        ()-> assertTrue(balanceCheck(user, 100, EUR)),
                        ()-> assertTrue(balanceCheck(user, 100, USD))
                )),



            dynamicTest("7. Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("8. Make a deposit of USD 100",
                ()-> accounts.deposit(user_id, 100, USD)),



            dynamicTest("9. Check that all balances are correct",
                ()-> assertAll("userBalancesCheck3",
                        ()-> assertTrue(balanceCheck(user, 0, GBP)),
                        ()-> assertTrue(balanceCheck(user, 100, EUR)),
                        ()-> assertTrue(balanceCheck(user, 200, USD))
                )),



            dynamicTest("10. Make a withdrawal of USD 200",
                ()-> accounts.withdraw(user_id, 200, USD)),



            dynamicTest("11. Check that all balances are correct",
                ()-> assertAll("userBalancesCheck4",
                        ()-> assertTrue(balanceCheck(user, 0, GBP)),
                        ()-> assertTrue(balanceCheck(user, 100, EUR)),
                        ()-> assertTrue(balanceCheck(user, 0, USD))
                )),



            dynamicTest("12. Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD)))
        );

    }

    private boolean balanceCheck(User user, int delta, String currencyName) throws Exception {
        Long cid = currency.getIdByName(currencyName);
        Long newAmount = user.getAccounts().get(cid).getAmount() + delta;
        String balance = accounts.balance(user.getId());
        String newBalance = String.format(" %d %s", newAmount, currency.getNameById(cid));
        return balance.contains(newBalance);
    }
}
