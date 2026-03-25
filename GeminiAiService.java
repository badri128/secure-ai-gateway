package com.aigateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class ClaudeAiService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    public ClaudeResult process(String input, String context, String agentType) {
        long start = System.currentTimeMillis();

        try {
            // Gemini API endpoint with key as query param
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            // Build system + user prompt (Gemini uses a single "contents" array)
            String systemPrompt = buildSystemPrompt(agentType, context);
            String fullPrompt = systemPrompt + "\n\nUser Input: " + input;

            // Build Gemini request body
            // {
            //   "contents": [{ "parts": [{ "text": "..." }] }]
            // }
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode contentItem = objectMapper.createObjectNode();
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", fullPrompt);
            parts.add(part);
            contentItem.set("parts", parts);
            contents.add(contentItem);
            requestBody.set("contents", contents);

            // Generation config
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", 1024);
            generationConfig.put("temperature", 0.7);
            requestBody.set("generationConfig", generationConfig);

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("Calling Gemini API | model={} | agent_type={}", model, agentType);
            log.info("Request body: {}", bodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Gemini API status: {}", response.statusCode());
            log.info("Gemini API response: {}", response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API returned " + response.statusCode() + " — " + response.body());
            }

            // Parse Gemini response
            // { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
            JsonNode json = objectMapper.readTree(response.body());
            String aiText = json
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // Gemini returns token counts in usageMetadata
            int inputTokens = json.path("usageMetadata").path("promptTokenCount").asInt(0);
            int outputTokens = json.path("usageMetadata").path("candidatesTokenCount").asInt(0);
            long elapsed = System.currentTimeMillis() - start;

            log.info("Gemini responded | tokens={} | ms={}", inputTokens + outputTokens, elapsed);
            return new ClaudeResult(aiText, inputTokens + outputTokens, elapsed);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI processing failed: " + e.getMessage());
        }
    }

    private String buildSystemPrompt(String agentType, String context) {
        String base = switch (agentType.toLowerCase()) {
            case "summarizer" -> "You are a precise summarization agent. Summarize the given input concisely in 2-3 sentences.";
            case "analyzer"   -> "You are a data analysis agent. Analyze the input and provide structured insights.";
            case "classifier" -> "You are a classification agent. Classify the input into relevant categories with reasoning.";
            default           -> "You are a helpful AI agent. Process the given input and provide a clear, helpful response.";
        };
        if (context != null && !context.isBlank()) {
            return base + "\n\nAdditional context: " + context;
        }
        return base;
    }

    public record ClaudeResult(String text, int tokensUsed, long processingTimeMs) {}
}
