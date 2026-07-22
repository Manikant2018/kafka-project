package com.api.logging.filter;

import com.api.logging.dto.ApiLog;
import com.api.logging.service.LogProducerService;
import com.api.logging.util.MaskingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final LogProducerService logProducerService;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_MDC = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .orElse(UUID.randomUUID().toString());
        MDC.put(CORRELATION_ID_MDC, requestId);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String traceId = MDC.get("traceId"); 

            ApiLog apiLog = ApiLog.builder()
                    .requestId(requestId)
                    .traceId(traceId)
                    .serviceName(serviceName)
                    .timestamp(LocalDateTime.now())
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .clientIp(request.getRemoteAddr())
                    .userAgent(request.getHeader("User-Agent"))
                    .headers(getHeaders(request))
                    .queryParams(getQueryParams(request))
                    .requestBody(MaskingUtil.maskBody(new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8)))
                    .responseBody(MaskingUtil.maskBody(new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8)))
                    .statusCode(response.getStatus())
                    .executionTime(duration)
                    .build();

            logProducerService.sendLog(apiLog);
            
            responseWrapper.copyBodyToResponse();
            MDC.clear();
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, MaskingUtil.mask(headerName, request.getHeader(headerName)));
        }
        return headers;
    }

    private Map<String, String> getQueryParams(HttpServletRequest request) {
        Map<String, String> queryParams = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> 
            queryParams.put(key, MaskingUtil.mask(key, String.join(",", value))));
        return queryParams;
    }
}
