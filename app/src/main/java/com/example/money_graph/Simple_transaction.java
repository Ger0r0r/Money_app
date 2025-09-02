package com.example.money_graph;

import java.util.Date;

public class Simple_transaction {
    private Date date;
    private String type;
    private String bank_account_from;
    private String transfer_to;
    private double transfer_amount;
    private String currency;
    private String transfer_note;

    public Simple_transaction(Date date, String type, String bank_account_from, String transfer_to, double transfer_amount, String currency, String transfer_note) {
        this.date = date;
        this.type = type;
        this.bank_account_from = bank_account_from;
        this.transfer_to = transfer_to;
        this.transfer_amount = transfer_amount;
        this.currency = currency;
        this.transfer_note = transfer_note;
    }

    public String getBank_account_from() {
        return bank_account_from;
    }
    public Date getDate() {
        return date;
    }
    public String getType() {
        return type;
    }
    public String getTransfer_to() {
        return transfer_to;
    }
    public double getTransfer_amount() {
        return transfer_amount;
    }
    public String getCurrency() {
        return currency;
    }
    public String getTransfer_note() {
        return transfer_note;
    }
}
