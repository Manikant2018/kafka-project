package com.api.logging.service;

import com.api.logging.dto.ApiLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_PREFIX = "api-logs-";

    public void sendLog(ApiLog apiLog) {
        // Dynamic topic name: api-logs-order-service
        String topicName = TOPIC_PREFIX + apiLog.getServiceName().toLowerCase().replace("-", "_");

        try {
            kafkaTemplate.send(topicName, apiLog.getRequestId(), apiLog)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Log sent to topic {} for RequestId: {}", topicName, apiLog.getRequestId());
                    } else {
                        log.error("Failed to send log to topic {} for RequestId: {}", topicName, apiLog.getRequestId(), ex);
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing log to Kafka topic {}", topicName, e);
        }
    }
}
