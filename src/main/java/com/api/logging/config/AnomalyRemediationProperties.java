package com.api.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "anomaly.remediation") // Removed @Component
@Data
public class AnomalyRemediationProperties {
    private Map<String, String> suggestions;
}
