package com.example.demo.services.Impl;

import com.example.demo.dao.AccountRepository;
import com.example.demo.exceptions.InsufficientFundsException;
import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.exceptions.UnknownCurrencyException;
import com.example.demo.models.Account;
import com.example.demo.models.User;
import com.example.demo.services.AccountManagmentService;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.services.UserManagmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.NoSuchElementException;

@Service
public class AccountManagmentServiceImpl implements AccountManagmentService {
    private static final Logger log = LoggerFactory.getLogger(AccountManagmentServiceImpl.class);

    private final AccountRepository repository;

    private final CurrencyManagmentService currencyService;

    private final UserManagmentService userService;

    public AccountManagmentServiceImpl(AccountRepository repository, CurrencyManagmentService currencyService, UserManagmentService userService) {
        this.repository = repository;
        this.currencyService = currencyService;
        this.userService = userService;
    }

    @Override
    public void initService() {}

    @Override
    public Account createAccount(User user, String currency_name) throws UnknownCurrencyException, NoSuchUserException {

        Long currency = currencyService.getByName(currency_name);
        if (currency==null) {
            throw new UnknownCurrencyException();
        }
        Account account = new Account(0, currency);


        User userNow = userService.getUserById(user.getId());
        if (userNow != null) {
            account.setUser(userNow);
            repository.save(account);
        }

        return account;
    }

    @Override
    public Account getById(long id){
        try {
            return repository.findById(id).get();
        } catch (NoSuchElementException ex){
            return null;
        }
    }

    @Override
    public void removeAccount(long id) {
        repository.deleteById(id);
    }

    @Override
    public String balance(long user_id) throws NoSuchUserException {
        User user = userService.getUserById(user_id);
        if (user != null) {
            StringBuffer result = new StringBuffer();

            user.getAccounts().forEach((account)->{
                result.append(String.format("Account #%d, Balance: %d %s ", account.getId(), account.getAmount(), currencyService.getById(account.getCurrency())));
                result.append(System.lineSeparator());
            });

            return result.toString();
        }

        throw new NoSuchUserException();
    }

    @Override
    public void deposit(long user_id, long amount, String currency) throws UnknownCurrencyException, NoSuchUserException {
        Long cid = currencyService.getByName(currency);
        if (cid == null) {
            throw new UnknownCurrencyException();
        }

        User user = userService.getUserById(user_id);
        user.getAccounts().forEach((account -> {
            if (account.getCurrency() == cid) {
                account.setAmount(account.getAmount() + Math.abs(amount));
                repository.saveAndFlush(account);
            }
        }));
    }

    @Override
    public void withdraw(long user_id, long amount, String currency) throws UnknownCurrencyException, InsufficientFundsException, NoSuchUserException {
        Long cid = currencyService.getByName(currency);
        if (cid == null) {
            throw new UnknownCurrencyException();
        }

        User user = userService.getUserById(user_id);

        for (Account account : user.getAccounts()) {
            if (account.getCurrency() == cid) {
                long newAmount = account.getAmount() - Math.abs(amount);

                if (newAmount >= 0) {
                    account.setAmount(newAmount);
                    repository.saveAndFlush(account);
                } else {
                    throw new InsufficientFundsException();
                }
            }
        }
    }
}
