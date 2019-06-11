package com.example.demo.dao;

import com.example.demo.dao.AccountRepository;
import com.example.demo.dao.CurrencyRepository;
import com.example.demo.dao.UserRepository;
import com.example.demo.models.Account;
import com.example.demo.models.Currency;
import com.example.demo.models.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class DaoTests {
    private final static long user_id = 1L;
    private final static long currency_id = 1L;
    private final static String currency_name = "GBP";

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
        entityManager.persist(new User(user_id, null));
        User user = users.findById(user_id).get();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getAccounts()).isNotNull();
        assertThat(user.getAccounts()).isNotEmpty();
    }

    @Test
    public void accountsCheck(){
        entityManager.persist(new Account(1L, 1L));
        Account account = accounts.findById(1L).get();
        assertThat(account.getId()).isEqualTo(1L);
        assertThat(account.getCurrency()).isEqualTo(1L);
        assertThat(account.getUser()).isNotNull();
        assertThat(account.getUser().getId()).isEqualTo(user_id);
    }

    @Test
    public void currencyCheck() {
        entityManager.detach(new Currency(currency_id, currency_name));
        Currency currency = currencies.findById(currency_id).get();
        assertThat(currency.getId()).isEqualTo(currency_id);
        assertThat(currency.getName()).isEqualTo(currency_name);
    }
}