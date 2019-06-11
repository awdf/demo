package com.example.demo.services;

public interface CurrencyManagmentService extends BaseService {
    String getById(long id);
    Long getByName(String name);
}
