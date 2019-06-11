package com.example.demo.exceptions;

public class InsufficientFundsException extends Exception {

    @Override
    public String getMessage() {
        return "Insufficient funds";
    }
}
