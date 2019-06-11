package com.example.demo.services;

import com.example.demo.models.Currency;

import java.util.List;

public interface CurrencyManagmentService extends BaseService {
    String getById(long id);
    Long getByName(String name);
    List<Currency> getAllCurrencies();
}
