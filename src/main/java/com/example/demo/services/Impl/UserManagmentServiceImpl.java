package com.example.demo.services.Impl;

import com.example.demo.dao.UserRepository;
import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.models.Account;
import com.example.demo.models.Currency;
import com.example.demo.models.User;
import com.example.demo.services.AccountManagmentService;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.services.UserManagmentService;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

@Service
public class UserManagmentServiceImpl implements UserManagmentService {
    private static final Logger log = LoggerFactory.getLogger(UserManagmentServiceImpl.class);

    @Autowired
    UserRepository repository;

    @Autowired
    CurrencyManagmentService currencyService;

    @Autowired
    AccountManagmentService accountService;

    Map<Long, User> usersCacheLayer = new LinkedTreeMap<>();

    @Override
    public void initService(){}

    @PostConstruct
    public void build() {

        // remove all users
        log.info("Remove old users:");
        log.info("-------------------------------");
        this.getAllUsers().forEach((user)->{
            log.info(user.toString());
            if (user.getId() != 1) {
                removeUser(user.getId());
            }
        });
        log.info("");
    }

    @Override
    public User createUser(Collection<Currency> currencies) {
        Map<Long, Account> accounts = new TreeMap<>();

        for (Currency currency : currencies) {
                accounts.put(currency.getId(), new Account(0,currency.getId()));
        }

        User user = new User(accounts.size(), accounts);
        accounts.forEach((c, account)->{
            account.setUser(user);
        });

        User created = repository.saveAndFlush(user);
        usersCacheLayer.put(created.getId(), created);
        return created;
    }

    @Override
    public void removeUser(long id) {
        usersCacheLayer.remove(id);
        repository.deleteById(id);
    }

    @Override
    public User getUserById(long id) throws NoSuchUserException {
        User cached = usersCacheLayer.get(id);
        if (cached != null) {
            cached.setAccount(cached.getAccounts().size());
            return cached;
        }

        try {
            return repository.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new NoSuchUserException();
        }
    }

    @Scheduled(initialDelay=10000, fixedRate = 10000)
    private void synchronize(){
        usersCacheLayer.forEach((id, user)->{
            if (user.getAccount() == 0) {
                repository.save(user);
                usersCacheLayer.remove(id);
            } else {
                user.setAccount(user.getAccount() - 1);
            }
        });
    }

    @PreDestroy
    public void onDestroy(){
        usersCacheLayer.forEach((id, user)-> repository.save(user));
        repository.flush();
        log.info("User service shutdown");
    }

    private List<User> getAllUsers(){
        return repository.findAll();
    }

}
