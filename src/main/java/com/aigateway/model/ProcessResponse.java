package com.aigateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProcessResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("agent_type")
    private String agentType;

    @JsonProperty("input_received")
    private String inputReceived;

    @JsonProperty("ai_response")
    private String aiResponse;

    @JsonProperty("ai_provider")
    private String aiProvider;

    @JsonProperty("model")
    private String model;

    @JsonProperty("tokens_used")
    private Integer tokensUsed;

    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("status")
    private String status;
}
