package com.example.parkio.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist.
 *
 * Keeps revoked JWT strings mapped to their expiry date. Expired entries are
 * purged lazily on every {@link #isBlacklisted} call.
 *
 * NOTE: single-node only. A restart or a second replica behind a load balancer
 * makes a "revoked" token valid again. Use {@link RedisTokenBlacklist}
 * (app.token-blacklist.store=redis) for any real deployment with more than
 * one instance.
 */
@Component
@ConditionalOnProperty(name = "app.token-blacklist.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Map<String, Date> blacklisted = new ConcurrentHashMap<>();

    @Override
    public void revoke(String token, Date expiry) {
        blacklisted.put(token, expiry);
    }

    @Override
    public boolean isBlacklisted(String token) {
        purgeExpired();
        return blacklisted.containsKey(token);
    }

    private void purgeExpired() {
        Date now = new Date();
        blacklisted.entrySet().removeIf(e -> e.getValue().before(now));
    }

    /** Exposed for testing / metrics. */
    public int size() {
        purgeExpired();
        return blacklisted.size();
    }
}
