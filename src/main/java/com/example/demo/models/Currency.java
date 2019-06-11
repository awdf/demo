package com.example.demo.models;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@Table(name="currency")
@EntityListeners(AuditingEntityListener.class)
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    private String Name;

    protected Currency(){}

    public Currency(long id, String name) {
        this.id = id;
        this.Name = name;
    }

    public long getId() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    @Override
    public String toString() {
        return "Currency{" +
                "ID=" + id +
                ", Name='" + Name + '\'' +
                '}';
    }
}
