package com.api.logging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyAlert {
    private String alertId;
    private LocalDateTime timestamp;
    private String serviceName;
    private String uri;
    private String anomalyType; // e.g., "HIGH_LATENCY", "HIGH_ERROR_RATE"
    private String description;
    private String severity; // e.g., "WARNING", "CRITICAL"
    private String requestId;
    private String traceId;
    private Map<String, String> remediationSuggestions;
    private Map<String, Object> additionalDetails; // e.g., actual_value, threshold_value
}
