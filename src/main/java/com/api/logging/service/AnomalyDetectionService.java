package com.api.logging.service;

import com.api.logging.config.AnomalyRemediationProperties;
import com.api.logging.dto.ApiLog;
import com.api.logging.dto.AnomalyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final AnomalyAlertProducer anomalyAlertProducer;
    private final AnomalyRemediationProperties remediationProperties; // Inject the new properties class

    // In a real system, these would be dynamic thresholds from a time-series DB or ML model
    // For PoC, we use a simple moving average and a deviation factor
    private final Map<String, Double> uriAvgExecutionTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> uriCallCount = new ConcurrentHashMap<>();
    private static final double LATENCY_DEVIATION_FACTOR = 2.0; // 2x the average is an anomaly
    private static final long MIN_CALLS_FOR_BASELINE = 10; // Need at least 10 calls to establish a baseline

    @Value("${anomaly.detection.threshold.execution-time-ms:500}")
    private long executionTimeThreshold; // Fallback or initial fixed threshold

    @KafkaListener(topicPattern = "api-logs-.*", groupId = "${spring.kafka.consumer.anomaly-group-id}")
    public void detectAnomalies(ApiLog apiLog) {
        log.debug("Anomaly Detector processing log for RequestId: {}", apiLog.getRequestId());

        String serviceUriKey = apiLog.getServiceName() + ":" + apiLog.getUri();

        // Update baseline for latency
        updateLatencyBaseline(serviceUriKey, apiLog.getExecutionTime());

        // Rule 1: High Latency Detection (dynamic threshold)
        if (apiLog.getExecutionTime() != null && uriCallCount.getOrDefault(serviceUriKey, 0) >= MIN_CALLS_FOR_BASELINE) {
            double average = uriAvgExecutionTime.get(serviceUriKey);
            double dynamicThreshold = average * LATENCY_DEVIATION_FACTOR;

            if (apiLog.getExecutionTime() > dynamicThreshold) {
                sendAnomalyAlert(apiLog, "HIGH_LATENCY", "API call took " + apiLog.getExecutionTime() + "ms, significantly higher than average " + String.format("%.2f", average) + "ms.",
                        "CRITICAL", Map.of("actual_execution_time", apiLog.getExecutionTime(), "average_execution_time", average, "dynamic_threshold", dynamicThreshold));
            }
        } else if (apiLog.getExecutionTime() != null && apiLog.getExecutionTime() > executionTimeThreshold) {
            // Fallback to fixed threshold if baseline not established
            sendAnomalyAlert(apiLog, "HIGH_LATENCY", "API call took " + apiLog.getExecutionTime() + "ms, exceeding fixed threshold of " + executionTimeThreshold + "ms (baseline not established).",
                    "WARNING", Map.of("actual_execution_time", apiLog.getExecutionTime(), "fixed_threshold", executionTimeThreshold));
        }

        // Rule 2: Server Error Status Code Detection (5xx errors)
        if (apiLog.getStatusCode() != null && apiLog.getStatusCode() >= 500 && apiLog.getStatusCode() < 600) {
            sendAnomalyAlert(apiLog, "SERVER_ERROR", "API call returned a server error: " + apiLog.getStatusCode(),
                    "CRITICAL", Map.of("status_code", apiLog.getStatusCode()));
        }

        // Rule 3: Client Error Rate Spike (e.g., too many 4xx errors - requires more complex state management)
        // For a true production system, this would involve counting 4xx errors over a time window
        // and comparing to a baseline or a fixed threshold.
        if (apiLog.getStatusCode() != null && apiLog.getStatusCode() >= 400 && apiLog.getStatusCode() < 500) {
            // This is a simplified example. A real system would track error rates over time.
            // For now, we'll just log a warning for individual 4xx errors.
            log.warn("Potential Client Error: {} | Service: {} | URI: {} | RequestId: {} | Status: {}",
                     apiLog.getServiceName(), apiLog.getUri(), apiLog.getRequestId(), apiLog.getStatusCode());
        }
    }

    private void updateLatencyBaseline(String serviceUriKey, Long currentExecutionTime) {
        if (currentExecutionTime == null || currentExecutionTime <= 0) return;

        uriCallCount.merge(serviceUriKey, 1, Integer::sum);
        uriAvgExecutionTime.merge(serviceUriKey, (double) currentExecutionTime, (oldAvg, newTime) -> {
            int count = uriCallCount.get(serviceUriKey);
            return (oldAvg * (count - 1) + newTime) / count;
        });
    }

    private void sendAnomalyAlert(ApiLog apiLog, String anomalyType, String description, String severity, Map<String, Object> additionalDetails) {
        AnomalyAlert alert = AnomalyAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .serviceName(apiLog.getServiceName())
                .uri(apiLog.getUri())
                .anomalyType(anomalyType)
                .description(description)
                .severity(severity)
                .requestId(apiLog.getRequestId())
                .traceId(apiLog.getTraceId())
                .remediationSuggestions(getRemediationSuggestions(anomalyType))
                .additionalDetails(additionalDetails)
                .build();

        anomalyAlertProducer.sendAnomalyAlert(alert);
    }

    private Map<String, String> getRemediationSuggestions(String anomalyType) {
        // Fetch suggestions from the injected properties class
        return Collections.singletonMap(anomalyType, remediationProperties.getSuggestions().getOrDefault(anomalyType, "No specific suggestion available for this anomaly type."));
    }
}
