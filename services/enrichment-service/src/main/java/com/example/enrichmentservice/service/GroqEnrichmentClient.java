package com.example.enrichmentservice.service;

import com.example.enrichmentservice.config.GroqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls Groq API (OpenAI-compatible, free tier) to generate title and description suggestions.
 * No local RAM usage — runs in the cloud. Returns null if API key is not set or request fails.
 */
@Component
public class GroqEnrichmentClient {

    private static final Logger logger = LoggerFactory.getLogger(GroqEnrichmentClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final GroqProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqEnrichmentClient(GroqProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Calls Groq to generate improved title and description. Returns null on any failure.
     */
    public AiEnrichmentService.EnrichmentResult generateEnrichment(String title, String description) {
        if (!properties.isEnabled()) {
            return null;
        }

        String userContent = buildUserPrompt(title, description);
        String requestBody = buildRequestBody(userContent);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getUrl()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                logger.warn("Groq API returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                logger.warn("Groq API returned no choices");
                return null;
            }

            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                logger.warn("Groq API returned empty content");
                return null;
            }

            return parseEnrichmentResponse(content, title, description);
        } catch (Exception e) {
            logger.warn("Groq enrichment request failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Offer to improve:\n");
        sb.append("Title: ").append(title != null ? title : "(empty)").append("\n");
        sb.append("Description: ").append(description != null ? description : "(empty)").append("\n\n");
        sb.append("Return a single JSON object with exactly these keys:\n");
        sb.append("- \"title\": improved, catchy listing title, max 140 characters, no quotes/emojis required\n");
        sb.append("- \"description\": improved description (can be longer), optionally add a short \"Benefits\" or \"Highlights\" section with bullet points\n");
        sb.append("- \"tags\": array of exactly 3 lowercase tags (e.g. [\"electronics\", \"phones\", \"deals\"])\n");
        sb.append("Respond only with the JSON object, no markdown or extra text.");
        return sb.toString();
    }

    private String buildRequestBody(String userContent) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("response_format", Map.of("type", "json_object"));
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "You are an expert at writing marketplace listing titles and descriptions. Reply only with valid JSON."),
                    Map.of("role", "user", "content", userContent)
            ));
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Groq request", e);
        }
    }

    private AiEnrichmentService.EnrichmentResult parseEnrichmentResponse(String content, String fallbackTitle, String fallbackDescription) {
        try {
            JsonNode json = objectMapper.readTree(content);
            String aiTitle = json.path("title").asText(null);
            String aiDescription = json.path("description").asText(null);
            List<String> aiTags = new ArrayList<>();
            JsonNode tagsNode = json.path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode t : tagsNode) {
                    String tag = t.asText(null);
                    if (tag != null && !tag.isBlank()) {
                        aiTags.add(tag.trim().toLowerCase());
                    }
                }
            }

            if (aiTitle == null || aiTitle.isBlank()) {
                aiTitle = fallbackTitle != null && !fallbackTitle.isBlank() ? fallbackTitle : "New Offer";
            }
            if (aiTitle.length() > 140) {
                aiTitle = aiTitle.substring(0, 140);
            }
            if (aiDescription == null || aiDescription.isBlank()) {
                aiDescription = fallbackDescription != null && !fallbackDescription.isBlank() ? fallbackDescription : "Quality product. Great value.";
            }
            if (aiTags.size() > 3) {
                aiTags = aiTags.subList(0, 3);
            }
            if (aiTags.size() < 3) {
                while (aiTags.size() < 3) {
                    aiTags.add("offer");
                }
            }

            return new AiEnrichmentService.EnrichmentResult(aiTitle, aiDescription, aiTags);
        } catch (Exception e) {
            logger.warn("Failed to parse Groq response: {}", e.getMessage());
            return null;
        }
    }
}
