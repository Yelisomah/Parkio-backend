package com.example.parkio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Puts a correlation ID on every request's MDC (so every log line in
 * @Slf4j classes for that request can be grepped together) and echoes it
 * back as a response header. Reuses an inbound X-Correlation-Id if the
 * caller (e.g. an API gateway) already set one.
 *
 * Runs first so downstream filters' log lines are tagged too.
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - startedAt);
            MDC.remove(MDC_KEY);
        }
    }
}
