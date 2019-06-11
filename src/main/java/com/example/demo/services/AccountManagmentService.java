package com.example.demo.services;

import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.models.Account;
import com.example.demo.models.User;

import java.util.List;

public interface AccountManagmentService extends BaseService {

    Account createAccount(User user, String currency) throws Exception;
    Account getById(long id);
    void removeAccount(long id);
    String balance(long user_id) throws Exception;
    void deposit(long user_id, long amount, String currency) throws Exception;
    void withdraw(long user_id, long amount, String currency) throws Exception;
}
