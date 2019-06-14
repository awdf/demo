package com.example.demo.services.Impl;

import com.example.demo.dao.AccountRepository;
import com.example.demo.exceptions.*;
import com.example.demo.models.Account;
import com.example.demo.models.User;
import com.example.demo.services.AccountManagmentService;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.services.UserManagmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.NoSuchElementException;

@Service
public class AccountManagmentServiceImpl implements AccountManagmentService {
    private static final Logger log = LoggerFactory.getLogger(AccountManagmentServiceImpl.class);
    private final boolean instantWrite = false;

    @Autowired
    private AccountRepository repository;

    @Autowired
    private CurrencyManagmentService currencyService;

    @Autowired
    private UserManagmentService userService;


    @Override
    public void initService() {}

    @Override
    public Account createAccount(User user, String currency_name) throws UnknownCurrencyException, NoSuchUserException {

        Long cid = currencyService.getIdByName(currency_name);
        if (cid==null) {
            throw new UnknownCurrencyException();
        }
        Account account = new Account(0, cid);


        User userNow = userService.getUserById(user.getId());
        if (userNow != null) {
            account.setUser(userNow);
            userNow.getAccounts().put(cid, account);
            repository.save(account);
        }

        return account;
    }

    @Override
    @Deprecated
    public Account getById(long id){
        try {
            return repository.findById(id).get();
        } catch (NoSuchElementException ex){
            return null;
        }
    }

    @Override
    @Deprecated
    public void removeAccount(long id) {
        repository.deleteById(id);
    }

    @Override
    public String balance(long user_id) throws NoSuchUserException {
        User user = userService.getUserById(user_id);

        StringBuffer result = new StringBuffer();
        user.getAccounts().forEach((cid, account)-> {
            try {
                result.append(String.format("Account #%d, Balance: %d %s ", account.getId(), account.getAmount(), currencyService.getNameById(cid)));
                result.append(System.lineSeparator());
            } catch (UnknownCurrencyException e) {
                e.printStackTrace();
            }
        });

        return result.toString();
    }

    @Override
    public void deposit(long user_id, long amount, String currency) throws UnknownCurrencyException, NoSuchUserException, NoSuchAccountException, AccountLimitReached {
        Long cid = currencyService.getIdByName(currency);
        User user = userService.getUserById(user_id);

        Account account = user.getAccounts().get(cid);
        if (account == null) {
            throw new NoSuchAccountException();
        }


        if (0 > Long.MAX_VALUE - account.getAmount() - Math.abs(amount)) {
            throw new AccountLimitReached();
        }

        account.setAmount(account.getAmount() + Math.abs(amount));
        writeToRepository(account);
    }

    @Override
    public void withdraw(long user_id, long amount, String currency) throws UnknownCurrencyException, InsufficientFundsException, NoSuchUserException, NoSuchAccountException {
        Long cid = currencyService.getIdByName(currency);
        User user = userService.getUserById(user_id);

        Account account = user.getAccounts().get(cid);
        if (account == null) {
            throw new NoSuchAccountException();
        }

        long newAmount = account.getAmount() - Math.abs(amount);
        if (0 > newAmount) {
            throw new InsufficientFundsException();
        }

        account.setAmount(newAmount);
        writeToRepository(account);
    }

    @PreDestroy
    public void onDestroy(){
        repository.flush();
        log.info("Account service shutdown");
    }

    private void writeToRepository(Account account){
        if (isInstantWrite()) {
            repository.save(account);
        }
    }

    public boolean isInstantWrite() {
        return instantWrite;
    }

}
