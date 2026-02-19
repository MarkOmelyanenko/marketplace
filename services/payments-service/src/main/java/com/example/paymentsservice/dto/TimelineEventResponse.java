package com.example.paymentsservice.dto;

import java.time.OffsetDateTime;

public class TimelineEventResponse {
    
    private OffsetDateTime at;
    private String type;
    private String details;
    
    public TimelineEventResponse() {
    }
    
    public TimelineEventResponse(OffsetDateTime at, String type, String details) {
        this.at = at;
        this.type = type;
        this.details = details;
    }
    
    public OffsetDateTime getAt() {
        return at;
    }
    
    public void setAt(OffsetDateTime at) {
        this.at = at;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}
