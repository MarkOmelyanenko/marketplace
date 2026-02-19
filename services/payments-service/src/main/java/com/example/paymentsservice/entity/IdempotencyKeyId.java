package com.example.paymentsservice.entity;

import java.io.Serializable;
import java.util.Objects;

public class IdempotencyKeyId implements Serializable {
    
    private String idempotencyKey;
    private String partnerId;
    
    public IdempotencyKeyId() {
    }
    
    public IdempotencyKeyId(String idempotencyKey, String partnerId) {
        this.idempotencyKey = idempotencyKey;
        this.partnerId = partnerId;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public String getPartnerId() {
        return partnerId;
    }
    
    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKeyId that = (IdempotencyKeyId) o;
        return Objects.equals(idempotencyKey, that.idempotencyKey) &&
               Objects.equals(partnerId, that.partnerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(idempotencyKey, partnerId);
    }
}
