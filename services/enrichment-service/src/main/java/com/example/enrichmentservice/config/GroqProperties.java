package com.example.enrichmentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.groq")
public class GroqProperties {

    private static final String DEFAULT_URL = "https://api.groq.com/openai/v1/chat/completions";

    /**
     * Groq API key (free at console.groq.com). If empty, enrichment falls back to rule-based suggestions.
     */
    private String apiKey = "";

    /**
     * Model name, e.g. llama-3.1-8b-instant, llama-3.3-70b-versatile.
     */
    private String model = "llama-3.1-8b-instant";

    /**
     * Override URL for chat completions (default: Groq OpenAI-compatible endpoint).
     */
    private String url = DEFAULT_URL;

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getModel() {
        return model != null && !model.isBlank() ? model : "llama-3.1-8b-instant";
    }

    public void setModel(String model) {
        this.model = model != null && !model.isBlank() ? model : "llama-3.1-8b-instant";
    }

    public String getUrl() {
        return url != null && !url.isBlank() ? url.trim() : DEFAULT_URL;
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url.trim();
    }

    public boolean isEnabled() {
        return getApiKey() != null && !getApiKey().isBlank();
    }
}
