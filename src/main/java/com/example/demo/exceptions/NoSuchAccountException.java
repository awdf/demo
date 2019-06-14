package com.example.demo.exceptions;

public class NoSuchAccountException extends Exception{
    @Override
    public String getMessage() {
        return "No such account";
    }
}
