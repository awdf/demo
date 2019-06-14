package com.example.demo.services.Impl;

import com.example.demo.dao.CurrencyRepository;
import com.example.demo.exceptions.UnknownCurrencyException;
import com.example.demo.models.Currency;
import com.example.demo.services.CurrencyManagmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
    @Scheduled(initialDelay=10000, fixedRate = 10000)
    private void  synchronize(){
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
    public String getNameById(long id) throws UnknownCurrencyException {
        String name = dictionary_id.get(id);
        if (name == null) {
            throw new UnknownCurrencyException();
        }
        return name;
    }

    @Override
    public Long getIdByName(String name) throws UnknownCurrencyException {
        Long cid = dictionary_name.get(name);
        if (cid == null) {
            throw new UnknownCurrencyException();
        }
        return cid;
    }

    @Override
    public List<Currency> getAllCurrencies(){
        return all;
    }
}
