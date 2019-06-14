package com.example.demo.services;

import com.example.demo.exceptions.UnknownCurrencyException;
import com.example.demo.models.Currency;

import java.util.List;

public interface CurrencyManagmentService extends BaseService {
    String getNameById(long id) throws UnknownCurrencyException;
    Long getIdByName(String name) throws UnknownCurrencyException;
    List<Currency> getAllCurrencies();
}
