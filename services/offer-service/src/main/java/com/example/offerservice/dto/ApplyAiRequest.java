package com.example.offerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplyAiRequest {
    
    @JsonProperty("useTitle")
    private Boolean useTitle;
    
    @JsonProperty("useDescription")
    private Boolean useDescription;
    
    public ApplyAiRequest() {
    }
    
    public ApplyAiRequest(Boolean useTitle, Boolean useDescription) {
        this.useTitle = useTitle;
        this.useDescription = useDescription;
    }
    
    public Boolean getUseTitle() {
        return useTitle;
    }
    
    public void setUseTitle(Boolean useTitle) {
        this.useTitle = useTitle;
    }
    
    public Boolean getUseDescription() {
        return useDescription;
    }
    
    public void setUseDescription(Boolean useDescription) {
        this.useDescription = useDescription;
    }
}
