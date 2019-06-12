package com.example.demo.services.Impl;

import com.example.demo.models.Currency;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.dao.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CurrencyManagmentServiceImpl implements CurrencyManagmentService {
    private static final Logger log = LoggerFactory.getLogger(CurrencyManagmentServiceImpl.class);

    private final CurrencyRepository repository;

    private Map<Long, String> dictionary_id;
    private Map<String, Long> dictionary_name;
    private List<Currency> all;

    public CurrencyManagmentServiceImpl(CurrencyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void initService(){

    }

    @PostConstruct
    private void build(){
        log.info("Currencies found with findAll():");
        log.info("-------------------------------");

        all = Collections.unmodifiableList(repository.findAll());

        HashMap<Long, String> d_i = new HashMap<>();
        HashMap<String, Long> d_n = new HashMap<>();
        for (Currency currency : all) {
            log.info(currency.toString());
            d_i.put(currency.getId(), currency.getName());
            d_n.put(currency.getName(), currency.getId());
        }

        dictionary_id = Collections.unmodifiableMap(d_i);
        dictionary_name = Collections.unmodifiableMap(d_n);

        log.info("");
    }

    @Override
    public String getById(long id){
        return dictionary_id.get(id);
    }

    @Override
    public Long getByName(String name) {
        return dictionary_name.get(name);
    }

    @Override
    public List<Currency> getAllCurrencies(){
        return all;
    }
}
