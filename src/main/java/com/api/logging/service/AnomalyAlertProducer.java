package com.api.logging.service;

import com.api.logging.dto.AnomalyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyAlertProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.anomaly-alerts}")
    private String anomalyAlertsTopic;

    public void sendAnomalyAlert(AnomalyAlert alert) {
        try {
            kafkaTemplate.send(anomalyAlertsTopic, alert.getAlertId(), alert)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Anomaly Alert sent to Kafka successfully: {} - {}", alert.getAlertId(), alert.getDescription());
                        } else {
                            log.error("Failed to send Anomaly Alert to Kafka: {} - {}", alert.getAlertId(), alert.getDescription(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing Anomaly Alert to Kafka", e);
        }
    }
}
