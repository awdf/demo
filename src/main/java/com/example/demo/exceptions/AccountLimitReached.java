package com.example.demo.exceptions;

public class AccountLimitReached extends Exception {
    @Override
    public String getMessage() {
        return "Account limit reached";
    }
}
