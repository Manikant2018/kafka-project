package com.api.logging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog extends BaseLog {
    private String method;
    private String uri;
    private String clientIp;
    private String userAgent;
    private String username;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String requestBody;
    private String responseBody;
    private Integer statusCode;
    private Long executionTime;
    private String errorMessage;
    private String stackTrace;
}
