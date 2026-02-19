package com.example.enrichmentservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Generates enrichment (AI title, description, tags) for offers. Uses Groq when configured,
 * otherwise deterministic rule-based fallback keyed by content hash.
 */
@Service
public class AiEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(AiEnrichmentService.class);

    private static final String[] TITLE_PREFIXES = { "🔥 ", "✨ ", "⭐ ", "💡 ", "🏷️ ", "✓ " };
    private static final String[] BENEFIT_HEADERS = { "✅ Benefits:", "📌 Highlights:", "👍 Why choose this:" };

    private final GroqEnrichmentClient groqEnrichmentClient;

    public AiEnrichmentService(GroqEnrichmentClient groqEnrichmentClient) {
        this.groqEnrichmentClient = groqEnrichmentClient;
    }

    /**
     * Generates AI suggestions: uses Groq API (free tier, no local RAM) when API key is set and request succeeds,
     * otherwise falls back to rule-based suggestions (same offer = same suggestion via hash seed).
     */
    public EnrichmentResult generateEnrichment(String title, String description) {
        logger.info("Generating AI enrichment for title: {}, description length: {}", title, description != null ? description.length() : 0);

        EnrichmentResult aiResult = groqEnrichmentClient != null ? groqEnrichmentClient.generateEnrichment(title, description) : null;
        if (aiResult != null) {
            logger.info("Using Groq-generated title and description");
            return aiResult;
        }

        int seed = contentSeed(title, description);
        String aiTitle = generateAiTitle(title, description, seed);
        String aiDescription = generateAiDescription(description, seed);
        List<String> aiTags = generateAiTags(title, description, seed);
        return new EnrichmentResult(aiTitle, aiDescription, aiTags);
    }

    private static int contentSeed(String title, String description) {
        String combined = (title != null ? title : "") + "|" + (description != null ? description : "");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Math.abs((hash[0] & 0xFF) | ((hash[1] & 0xFF) << 8) | ((hash[2] & 0xFF) << 16));
        } catch (NoSuchAlgorithmException e) {
            return combined.hashCode() & 0x7FFF_FFFF;
        }
    }

    private String generateAiTitle(String title, String description, int seed) {
        String prefix = TITLE_PREFIXES[seed % TITLE_PREFIXES.length];

        if (title == null || title.trim().isEmpty()) {
            if (description != null && !description.trim().isEmpty()) {
                String derived = description.trim();
                if (derived.length() > 137) {
                    derived = derived.substring(0, 137) + "...";
                }
                return prefix + derived;
            }
            return prefix + "New Offer";
        }

        String trimmed = title.trim();
        if (trimmed.length() > 137) {
            trimmed = trimmed.substring(0, 137) + "...";
        }
        return prefix + trimmed;
    }

    private String generateAiDescription(String description, int seed) {
        String header = BENEFIT_HEADERS[seed % BENEFIT_HEADERS.length];

        if (description == null || description.trim().isEmpty()) {
            List<String> generic = new ArrayList<>(List.of("Great value", "Quality product", "Fast delivery"));
            Collections.rotate(generic, seed % 3);
            return header + "\n• " + String.join("\n• ", generic.subList(0, 3));
        }

        StringBuilder enhanced = new StringBuilder(description.trim());
        enhanced.append("\n\n").append(header);

        String lowerDesc = description.toLowerCase();
        List<String> benefits = new ArrayList<>();

        if (lowerDesc.contains("phone") || lowerDesc.contains("mobile") || lowerDesc.contains("smartphone")) {
            benefits.add("Latest technology and features");
            benefits.add("Long battery life");
            benefits.add("High-quality display");
            benefits.add("Premium build quality");
            benefits.add("Great camera");
        } else if (lowerDesc.contains("laptop") || lowerDesc.contains("computer")) {
            benefits.add("Powerful performance");
            benefits.add("Fast processing speed");
            benefits.add("Reliable and durable");
            benefits.add("Portable design");
            benefits.add("Long battery life");
        } else if (lowerDesc.contains("book") || lowerDesc.contains("reading")) {
            benefits.add("Engaging content");
            benefits.add("Easy to read");
            benefits.add("Great value");
            benefits.add("Knowledge at your fingertips");
            benefits.add("Perfect for learning");
        } else if (lowerDesc.contains("clothing") || lowerDesc.contains("shirt") || lowerDesc.contains("dress") || lowerDesc.contains("fashion")) {
            benefits.add("Comfortable fit");
            benefits.add("High-quality materials");
            benefits.add("Stylish design");
            benefits.add("Durable and washable");
            benefits.add("Trendy and versatile");
        } else {
            benefits.add("Great value for money");
            benefits.add("Quality product");
            benefits.add("Fast delivery available");
            benefits.add("Customer favorite");
            benefits.add("Highly rated");
        }
        Collections.rotate(benefits, seed % Math.max(1, benefits.size()));
        int count = Math.min(3, benefits.size());
        for (int i = 0; i < count; i++) {
            enhanced.append("\n• ").append(benefits.get(i));
        }

        return enhanced.toString();
    }

    private List<String> generateAiTags(String title, String description, int seed) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ROOT);

        List<String> tags = new ArrayList<>();

        if (combined.contains("phone") || combined.contains("mobile") || combined.contains("smartphone")) {
            tags.add("electronics");
            tags.add("mobile");
            tags.add("smartphone");
        } else if (combined.contains("laptop") || combined.contains("computer") || combined.contains("pc")) {
            tags.add("electronics");
            tags.add("computers");
            tags.add("laptops");
        } else if (combined.contains("book") || combined.contains("reading")) {
            tags.add("books");
            tags.add("education");
            tags.add("reading");
        } else if (combined.contains("clothing") || combined.contains("shirt") || combined.contains("dress") || combined.contains("fashion")) {
            tags.add("fashion");
            tags.add("clothing");
            tags.add("apparel");
        } else {
            String[][] defaultTagSets = {
                { "marketplace", "offer", "new" },
                { "featured", "popular", "bestseller" },
                { "quality", "value", "deals" },
                { "discover", "explore", "find" },
            };
            String[] set = defaultTagSets[seed % defaultTagSets.length];
            for (String t : set) {
                tags.add(t);
            }
        }

        return tags.subList(0, Math.min(3, tags.size()));
    }

    public static class EnrichmentResult {
        private final String aiTitle;
        private final String aiDescription;
        private final List<String> aiTags;

        public EnrichmentResult(String aiTitle, String aiDescription, List<String> aiTags) {
            this.aiTitle = aiTitle;
            this.aiDescription = aiDescription;
            this.aiTags = aiTags;
        }

        public String getAiTitle() {
            return aiTitle;
        }

        public String getAiDescription() {
            return aiDescription;
        }

        public List<String> getAiTags() {
            return aiTags;
        }
    }
}
