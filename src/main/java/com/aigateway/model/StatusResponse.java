package com.aigateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusResponse {

    private String service;

    private String status;

    private String version;

    @JsonProperty("uptime_seconds")
    private long uptimeSeconds;

    @JsonProperty("ai_provider")
    private String aiProvider;

    @JsonProperty("ai_model")
    private String aiModel;

    private Instant timestamp;

    private String environment;
}
