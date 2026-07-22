package com.api.logging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogRetentionService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${logging.retention.days:30}")
    private int retentionDays;

    /**
     * Runs every day at 1 AM to purge old logs from all service-specific tables.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void purgeOldLogs() {
        log.info("Starting log retention purge for logs older than {} days", retentionDays);

        try {
            // Find all tables that follow our naming convention
            String findTablesSql = "SELECT table_name FROM information_schema.tables " +
                                  "WHERE table_schema = 'public' AND table_name LIKE 'api_logs_%'";
            
            List<String> tableNames = jdbcTemplate.queryForList(findTablesSql, String.class);

            for (String tableName : tableNames) {
                log.debug("Purging old logs from table: {}", tableName);
                
                String deleteSql = "DELETE FROM " + tableName + " WHERE timestamp < NOW() - INTERVAL '" + retentionDays + " days'";
                int deletedRows = jdbcTemplate.update(deleteSql);
                
                if (deletedRows > 0) {
                    log.info("Purged {} records from {}", deletedRows, tableName);
                }
            }
            
            log.info("Log retention purge completed successfully.");
            
            // Optional: Run VACUUM to reclaim disk space (specific to PostgreSQL)
            // jdbcTemplate.execute("VACUUM ANALYZE");

        } catch (Exception e) {
            log.error("Error during log retention purge: {}", e.getMessage(), e);
        }
    }
}
