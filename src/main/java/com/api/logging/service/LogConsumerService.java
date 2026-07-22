package com.api.logging.service;

import com.api.logging.dto.ApiLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogConsumerService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Set<String> initializedTables = Collections.synchronizedSet(new HashSet<>());

    // topicPattern = "api-logs-.*" matches any topic starting with "api-logs-"
    @KafkaListener(topicPattern = "api-logs-.*", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeApiLog(ApiLog apiLog) {
        String serviceName = apiLog.getServiceName() != null ? apiLog.getServiceName() : "unknown_service";
        String tableName = "api_logs_" + serviceName.toLowerCase().replace("-", "_");

        try {
            ensureTableExists(tableName);

            String sql = "INSERT INTO " + tableName + " " +
                    "(request_id, trace_id, service_name, timestamp, method, uri, status_code, execution_time, " +
                    "client_ip, user_agent, headers, query_params, request_body, response_body, error_message, stack_trace) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    apiLog.getRequestId(),
                    apiLog.getTraceId(),
                    apiLog.getServiceName(),
                    apiLog.getTimestamp(),
                    apiLog.getMethod(),
                    apiLog.getUri(),
                    apiLog.getStatusCode(),
                    apiLog.getExecutionTime(),
                    apiLog.getClientIp(),
                    apiLog.getUserAgent(),
                    objectMapper.writeValueAsString(apiLog.getHeaders()),
                    objectMapper.writeValueAsString(apiLog.getQueryParams()),
                    apiLog.getRequestBody(),
                    apiLog.getResponseBody(),
                    apiLog.getErrorMessage(),
                    apiLog.getStackTrace()
            );

            log.info("Saved log from topic to table: {} | RequestId: {}", tableName, apiLog.getRequestId());

        } catch (Exception e) {
            log.error("Error processing log for table {}: {}", tableName, e.getMessage());
        }
    }

    private void ensureTableExists(String tableName) {
        if (initializedTables.contains(tableName)) return;

        synchronized (initializedTables) {
            if (initializedTables.contains(tableName)) return;

            String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "request_id VARCHAR(50), " +
                    "trace_id VARCHAR(50), " +
                    "service_name VARCHAR(100), " +
                    "timestamp TIMESTAMP, " +
                    "method VARCHAR(10), " +
                    "uri TEXT, " +
                    "status_code INTEGER, " +
                    "execution_time BIGINT, " +
                    "client_ip VARCHAR(45), " +
                    "user_agent TEXT, " +
                    "headers JSONB, " +
                    "query_params JSONB, " +
                    "request_body TEXT, " +
                    "response_body TEXT, " +
                    "error_message TEXT, " +
                    "stack_trace TEXT" +
                    ")";
            
            jdbcTemplate.execute(createTableSql);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + tableName + "_trace_id ON " + tableName + "(trace_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + tableName + "_timestamp ON " + tableName + "(timestamp)");
            
            initializedTables.add(tableName);
            log.info("Initialized table: {}", tableName);
        }
    }
}
