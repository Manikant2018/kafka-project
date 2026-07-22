package com.api.logging.controller;

import com.api.logging.dto.TestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Slf4j
@RequiredArgsConstructor
public class TestController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/hello")
    public ResponseEntity<String> helloWorld(@RequestParam(required = false) String name) {
        String message = "Hello, " + (name != null ? name : "World") + "!";
        log.info("Responding to /hello with: {}", message);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitData(@RequestBody TestRequest request) {
        log.info("Received data for /submit: {}", request.getEmail());
        return ResponseEntity.ok("Data submitted successfully for " + request.getName());
    }

    @GetMapping("/logs/{serviceName}")
    public ResponseEntity<List<Map<String, Object>>> getLogsForService(@PathVariable String serviceName) {
        String tableName = "api_logs_" + serviceName.toLowerCase().replace("-", "_");
        try {
            String sql = "SELECT * FROM " + tableName + " ORDER BY timestamp DESC LIMIT 100";
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Table {} not found or error querying: {}", tableName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
