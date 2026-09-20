package com.example.parkio.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP token-bucket rate limiter using Bucket4j 8.x API.
 *
 * <p>Limits:
 * <ul>
 *   <li>Auth endpoints (/api/v1/auth/**): 20 req / min — brute-force protection</li>
 *   <li>All other API endpoints: 200 req / min</li>
 * </ul>
 *
 * <p>NOTE: Buckets are stored in-memory. Replace with a Redis-backed
 * ProxyManager for multi-node deployments.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = resolveClientIp(request);
        boolean isAuth = request.getRequestURI().startsWith("/api/v1/auth");
        String key = ip + ":" + (isAuth ? "auth" : "api");

        Bucket bucket = buckets.computeIfAbsent(key, k -> isAuth ? buildAuthBucket() : buildApiBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", ip, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please slow down.\"}");
        }
    }

    private Bucket buildAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillGreedy(20, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket buildApiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(200)
                        .refillGreedy(200, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
