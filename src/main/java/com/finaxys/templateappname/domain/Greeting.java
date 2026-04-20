package com.finaxys.templateappname.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "greeting")
public class Greeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "key" is a SQL reserved word — mapped to a safe column name
    @Column(name = "greeting_key", length = 50, unique = true, nullable = false)
    private String key;

    @Column(length = 255, nullable = false)
    private String message;

    protected Greeting() {
        // Required by JPA
    }

    public Greeting(String key, String message) {
        this.key = key;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
