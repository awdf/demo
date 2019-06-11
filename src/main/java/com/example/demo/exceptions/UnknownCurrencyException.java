package com.example.demo.exceptions;

public class UnknownCurrencyException extends Exception {

    @Override
    public String getMessage() {
        return "Unknown currency";
    }
}
