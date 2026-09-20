package com.example.parkio.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

/**
 * Redis-backed token blacklist.
 *
 * Each revoked token is stored as {@code blacklist:token:<jwt>} with a TTL
 * equal to its remaining lifetime, so Redis itself expires the entry — no
 * manual purge needed. Shared across every app instance, so logout works
 * correctly behind a load balancer and survives app restarts.
 *
 * Active when {@code app.token-blacklist.store=redis}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.token-blacklist.store", havingValue = "redis")
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String KEY_PREFIX = "blacklist:token:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void revoke(String token, Date expiry) {
        long ttlMillis = expiry.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return; // already expired, nothing to blacklist
        }
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token, "1", Duration.ofMillis(ttlMillis));
        } catch (Exception ex) {
            // If Redis is briefly unavailable, fail safe by logging rather than
            // throwing — losing a single logout revocation is far less harmful
            // than a 500 on every authenticated request.
            log.error("Failed to write token revocation to Redis", ex);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
        } catch (Exception ex) {
            log.error("Failed to check token revocation in Redis; failing open", ex);
            return false;
        }
    }
}
