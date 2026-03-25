package com.aigateway.controller;

import com.aigateway.model.ProcessRequest;
import com.aigateway.model.ProcessResponse;
import com.aigateway.model.StatusResponse;
import com.aigateway.service.GeminiAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AiGatewayController {

    private final GeminiAiService geminiAiService;

    private static final long START_TIME = System.currentTimeMillis();

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String aiModel;

    /**
     * POST /process-data
     *
     * Sample request body:
     * {
     *   "input": "What are the benefits of microservices?",
     *   "context": "Enterprise software context",
     *   "agent_type": "summarizer"
     * }
     *
     * agent_type options: summarizer | analyzer | classifier | general
     */
    @PostMapping("/process-data")
    public ResponseEntity<ProcessResponse> processData(@Valid @RequestBody ProcessRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Processing request | id={} | agent_type={}", requestId, request.getAgentType());

        GeminiAiService.AiResult result = geminiAiService.process(
                request.getInput(),
                request.getContext(),
                request.getAgentType()
        );

        ProcessResponse response = ProcessResponse.builder()
                .requestId(requestId)
                .agentType(request.getAgentType())
                .inputReceived(request.getInput())
                .aiResponse(result.text())
                .aiProvider("Groq LLaMA")
                .model(result.model())
                .tokensUsed(result.tokensUsed())
                .processingTimeMs(result.processingTimeMs())
                .timestamp(Instant.now())
                .status("SUCCESS")
                .build();

        log.info("Request completed | id={} | tokens={}", requestId, result.tokensUsed());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /status
     * Health check endpoint — also used by Apigee's health monitor
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        long uptimeSeconds = (System.currentTimeMillis() - START_TIME) / 1000;

        StatusResponse status = StatusResponse.builder()
                .service("ai-agent-gateway")
                .status("UP")
                .version("1.0.0")
                .uptimeSeconds(uptimeSeconds)
                .aiProvider("Groq LLaMA")
                .aiModel(aiModel)
                .timestamp(Instant.now())
                .environment(activeProfile)
                .build();

        return ResponseEntity.ok(status);
    }

    // Global exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Request failed: {}", e.getMessage());
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("PROCESSING_FAILED", e.getMessage()));
    }

    public record ErrorResponse(String error, String message) {}
}
