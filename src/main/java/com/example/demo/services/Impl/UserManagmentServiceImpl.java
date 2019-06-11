package com.example.demo.services.Impl;

import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.models.Account;
import com.example.demo.models.Currency;
import com.example.demo.models.User;
import com.example.demo.services.AccountManagmentService;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.services.UserManagmentService;
import com.example.demo.dao.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserManagmentServiceImpl implements UserManagmentService {
    private static final Logger log = LoggerFactory.getLogger(UserManagmentServiceImpl.class);

    @Autowired
    UserRepository repository;

    @Autowired
    CurrencyManagmentService currencyService;

    @Autowired
    AccountManagmentService accountService;

    @Override
    public void initService(){
        // remove all users
        log.info("Users found with getAllUsers:");
        log.info("-------------------------------");
        this.getAllUsers().forEach((user)->{
            log.info(user.toString());
            if (user.getId() != 1) {
                try {
                    removeUser(user.getId());
                } catch (NoSuchUserException e) {
                    e.printStackTrace();
                }
            }
        });
        log.info("");
    }

    @Override
    public User createUser(List<Currency> currencies) {
        List<Account> accounts = new LinkedList<>();

        for (Currency currency : currencies) {
                accounts.add(new Account(0,currency.getId()));
        }

        User user = new User(accounts.size(), accounts);
        accounts.forEach((account)->{
            account.setUser(user);
        });

        return repository.saveAndFlush(user);
    }

    @Override
    public void removeUser(long id) throws NoSuchUserException {
        User user = getUserById(id);
        if (user != null) {
            repository.delete(user);
        }
    }

    @Override
    public User getUserById(long id) throws NoSuchUserException {
        try {
            return repository.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new NoSuchUserException();
        }

    }

    @Override
    public List<User> getAllUsers(){
        return repository.findAll();
    }
}
