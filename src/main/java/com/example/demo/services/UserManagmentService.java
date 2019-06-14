package com.example.demo.services;

import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.models.Currency;
import com.example.demo.models.User;

import java.util.Collection;


public interface UserManagmentService extends BaseService {
    User createUser(Collection<Currency> currencies);
    void removeUser(long id) throws NoSuchUserException;
    User getUserById(long id) throws NoSuchUserException;
}
