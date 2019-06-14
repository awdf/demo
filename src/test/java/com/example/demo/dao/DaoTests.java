package com.example.demo.dao;

import com.example.demo.models.Account;
import com.example.demo.models.Currency;
import com.example.demo.models.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class DaoTests {
    private static final Logger log = LoggerFactory.getLogger(DaoTests.class);

    private final static long user_id = 1L;
    private final static long currency_id = 1L;
    private final static String GBP = "GBP";
    private final static String EUR = "EUR";
    private final static String USD = "USD";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    UserRepository users;

    @Autowired
    CurrencyRepository currencies;

    @Autowired
    AccountRepository accounts;

    @Test
    public void userCheck(){
        assertThat(users).isNotNull();

        entityManager.persist(new User(user_id, null));
        User user = users.findById(user_id).get();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getAccounts()).isNotNull();
        assertThat(user.getAccounts()).isNotEmpty();
    }

    @Test
    public void accountsCheck(){
        assertThat(accounts).isNotNull();

        entityManager.persist(new Account(1L, 1L));
        Account account = accounts.findById(1L).get();
        assertThat(account.getId()).isEqualTo(1L);
        assertThat(account.getCurrency()).isEqualTo(1L);
        assertThat(account.getUser()).isNotNull();
        assertThat(account.getUser().getId()).isEqualTo(user_id);
    }

    @Test
    public void currencyCheck() {
        assertThat(currencies).isNotNull();

        entityManager.detach(new Currency(currency_id, GBP));
        Currency currency = currencies.findById(currency_id).get();
        assertThat(currency.getId()).isEqualTo(currency_id);
        assertThat(currency.getName()).isEqualTo(GBP);
    }

    @Test
    public void performanceCheck(){
        assertThat(users).isNotNull();

        long operations = 0;
        Instant start = Instant.now();
        do {
            operations++;
            Map<Long, Account> accounts = new HashMap<>();
            accounts.put(1L, new Account(0, 1));
            accounts.put(2L, new Account(0, 2));
            accounts.put(3L, new Account(0, 3));
            User user = new User(3L, accounts);
            accounts.forEach((c, account) -> account.setUser(user));

            User created = users.saveAndFlush(user);
            Account any = created.getAccounts().get(1l);
            any.setAmount(1000);
            users.saveAndFlush(created);
            users.deleteById(created.getId());
        } while (Duration.between(start, Instant.now()).toMillis() < 1000);
        log.info("CRUD Performance {} (ops): ", operations);
    }
}