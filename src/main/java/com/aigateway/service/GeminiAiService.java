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
public class GeminiAiService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    public record AiResult(String text, int tokensUsed, long processingTimeMs, String model) {}

    /**
     * Calls Groq API (OpenAI-compatible format).
     * Free tier: 30 req/min, 14,400 req/day — no credit card, works in India.
     * Docs: https://console.groq.com/docs/openai
     */
    public AiResult process(String input, String context, String agentType) {
        long start = System.currentTimeMillis();

        try {
            /*
             * Groq uses OpenAI-compatible /chat/completions format:
             * {
             *   "model": "llama-3.3-70b-versatile",
             *   "messages": [
             *     { "role": "system", "content": "..." },
             *     { "role": "user",   "content": "..." }
             *   ],
             *   "max_tokens": 1024
             * }
             */
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            ArrayNode messages = objectMapper.createArrayNode();

            // System message
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemPrompt(agentType, context));
            messages.add(systemMsg);

            // User message
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", input);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("Calling Groq API | model={} | agent_type={}", model, agentType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Groq API status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("Groq API error: {}", response.body());
                throw new RuntimeException("Groq API returned " + response.statusCode() + " — " + response.body());
            }

            /*
             * Groq success response (OpenAI format):
             * {
             *   "choices": [{ "message": { "content": "AI response here" } }],
             *   "usage": { "prompt_tokens": 45, "completion_tokens": 120, "total_tokens": 165 }
             * }
             */
            JsonNode json = objectMapper.readTree(response.body());

            String aiText = json
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText("No response generated");

            int totalTokens = json.path("usage").path("total_tokens").asInt(0);
            long elapsed = System.currentTimeMillis() - start;

            log.info("Groq responded | tokens={} | ms={}", totalTokens, elapsed);
            return new AiResult(aiText, totalTokens, elapsed, model);

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI processing failed: " + e.getMessage());
        }
    }

    private String buildSystemPrompt(String agentType, String context) {
        String base = switch (agentType.toLowerCase()) {
            case "summarizer" ->
                "You are a precise summarization agent. Summarize the given input concisely in 2-3 sentences. Be direct and factual.";
            case "analyzer" ->
                "You are a data analysis agent. Analyze the input and provide structured insights with key findings.";
            case "classifier" ->
                "You are a classification agent. Classify the input into relevant categories and explain your reasoning.";
            default ->
                "You are a helpful AI agent. Process the given input and provide a clear, helpful response.";
        };
        if (context != null && !context.isBlank()) {
            return base + "\n\nAdditional Context: " + context;
        }
        return base;
    }
}
