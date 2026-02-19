package com.example.paymentsservice.dto;

public class WalletBalanceResponse {

    private long balanceCents;
    private String currency;

    public WalletBalanceResponse() {
    }

    public WalletBalanceResponse(long balanceCents, String currency) {
        this.balanceCents = balanceCents;
        this.currency = currency;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(long balanceCents) {
        this.balanceCents = balanceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
