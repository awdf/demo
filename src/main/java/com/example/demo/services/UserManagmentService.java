package com.example.demo.services;

import com.example.demo.exceptions.NoSuchUserException;
import com.example.demo.models.User;

import java.util.List;

public interface UserManagmentService extends BaseService {
    User createUser(String ... currencies);
    void removeUser(long id) throws NoSuchUserException;
    User getUserById(long id) throws NoSuchUserException;
    List<User> getAllUsers();
}