package com.example.demo.Services;

import com.example.demo.exceptions.InsufficientFundsException;
import com.example.demo.exceptions.NoSuchAccountException;
import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.exceptions.UnknownCurrencyException;
import com.example.demo.models.Currency;
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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

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
            accounts.deposit(user.getId(), 2, GBP);
            accounts.withdraw(user.getId(), 1, GBP);
            accounts.balance(user.getId());
            operations++;
        } while (Duration.between(start, Instant.now()).toMillis() < 1000);
        log.info("Service performance {} (DWB ops) per {} ms ", operations, Duration.between(start, Instant.now()).toMillis());

        assertEquals(operations, parseBalance(user, GBP));
        users.removeUser(user.getId());
    }

    @Test
    @DisplayName("Service exceptions test")
    public void exceptionsCheck() throws NoSuchUserException {
        final List<Currency> currencies = new LinkedList<>();
        currencies.add(new Currency(1, GBP));
        currencies.add(new Currency(2, EUR));

        User user = users.createUser(currencies);
        long id = user.getId();

        assertThrows(NoSuchUserException.class, () -> accounts.deposit(Long.MAX_VALUE, 0, GBP));
        assertThrows(UnknownCurrencyException.class, () -> accounts.deposit(id, 0, "UAH"));

        assertThrows(NoSuchUserException.class, () -> accounts.withdraw(Long.MAX_VALUE, 0, GBP));
        assertThrows(UnknownCurrencyException.class, () -> accounts.withdraw(id, 0, "UAH"));
        assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(id, Long.MAX_VALUE, GBP));
        assertThrows(NoSuchAccountException.class, () -> accounts.withdraw(user.getId(), 0, USD));

        assertThrows(NoSuchUserException.class, () -> accounts.balance(Long.MAX_VALUE));

        users.removeUser(user.getId());
    }

    @TestFactory
    @DisplayName("Sequence of actions test")
    Collection<DynamicTest> accountsActions() throws NoSuchUserException, UnknownCurrencyException {

        assertNotNull(users);
        assertNotNull(accounts);
        assertNotNull(currency);

        User user = users.getUserById(user_id);
        long balanceGBP = user.getAccounts().get(currency.getIdByName(GBP)).getAmount();
        long balanceEUR = user.getAccounts().get(currency.getIdByName(EUR)).getAmount();
        long balanceUSD = user.getAccounts().get(currency.getIdByName(USD)).getAmount();
        assertEquals(0L, balanceGBP); //Remove for non Zero amount
        assertEquals(0L, balanceEUR); //Remove for non Zero amount
        assertEquals(0L, balanceUSD); //Remove for non Zero amount

        return Arrays.asList(
            dynamicTest("1.Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("2.Make a deposit of USD 100",
                ()-> accounts.deposit(user_id, 100, USD)),



            dynamicTest("3.Check that all balances are correct",
                ()-> assertAll("userBalancesCheck1",
                    ()-> assertEquals(balanceGBP + 000, parseBalance(user, GBP)),
                    ()-> assertEquals(balanceEUR + 000, parseBalance(user, EUR)),
                    ()-> assertEquals(balanceUSD + 100, parseBalance(user, USD))
                )),



            dynamicTest("4. Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("5. Make a deposit of EUR 100",
                ()-> accounts.deposit(user_id, 100, EUR)),



            dynamicTest("6. Check that all balances are correct",
                ()-> assertAll("userBalancesCheck2",
                        ()-> assertEquals(balanceGBP + 000, parseBalance(user, GBP)),
                        ()-> assertEquals(balanceEUR + 100, parseBalance(user, EUR)),
                        ()-> assertEquals(balanceUSD + 100, parseBalance(user, USD))
                )),



            dynamicTest("7. Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("8. Make a deposit of USD 100",
                ()-> accounts.deposit(user_id, 100, USD)),



            dynamicTest("9. Check that all balances are correct",
                ()-> assertAll("userBalancesCheck3",
                        ()-> assertEquals(balanceGBP + 000, parseBalance(user, GBP)),
                        ()-> assertEquals(balanceEUR + 100, parseBalance(user, EUR)),
                        ()-> assertEquals(balanceUSD + 200, parseBalance(user, USD))
                )),



            dynamicTest("10. Make a withdrawal of USD 200",
                ()-> accounts.withdraw(user_id, 200, USD)),



            dynamicTest("11. Check that all balances are correct",
                ()-> assertAll("userBalancesCheck4",
                        ()-> assertEquals(balanceGBP + 000, parseBalance(user, GBP)),
                        ()-> assertEquals(balanceEUR + 100, parseBalance(user, EUR)),
                        ()-> assertEquals(balanceUSD + 000, parseBalance(user, USD))
                )),



            dynamicTest("12. Make a withdrawal of USD 200",
                ()-> assertThrows(InsufficientFundsException.class, () -> accounts.withdraw(user_id, 200, USD))),



            dynamicTest("Keep clean",
                    () -> accounts.withdraw(user_id, 100, EUR))
        );

    }

    private long parseBalance(User user, String currencyName) throws Exception {
        int cid = currency.getIdByName(currencyName).intValue();
        String balance = accounts.balance(user.getId());
        String[] split = balance.replaceAll("#\\d+,|[A-Za-z,\\s]", "").split(":");
        return Long.parseLong(split[cid]);
    }
}
