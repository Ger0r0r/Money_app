package com.example.money_graph;

import java.util.Date;

public class Balance_transaction {
    private String type;
    private Date date;
    private int index;
    private double amount;
    private String currency;

    public Balance_transaction (String type, Date date, int index, double amount, String currency) {
        this.type = type;
        this.date = date;
        this.index = index;
        this.amount = amount;
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }
    public int getIndex() {
        return index;
    }
    public String getType() {
        return type;
    }
    public double getAmount() {
        return amount;
    }
    public Date getDate() {
        return date;
    }
}
