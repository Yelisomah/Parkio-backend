package com.example.parkio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Simple Redis distributed lock (SET NX PX + token-checked release via Lua).
 *
 * This is the mechanism tech-standards.md calls a non-negotiable: "Any code
 * path that confirms a booking must acquire the space/slot Redis lock first
 * and release it on every exit path (success, failure, exception, timeout)."
 *
 * Usage:
 * <pre>
 *   String token = lockService.tryAcquire(key, Duration.ofSeconds(10));
 *   if (token == null) throw ParkioException.conflict("Someone else is booking this spot right now");
 *   try {
 *       ... critical section ...
 *   } finally {
 *       lockService.release(key, token);
 *   }
 * </pre>
 *
 * NOTE: this is a single-Redis-instance lock (adequate for one primary Redis /
 * a managed Redis with failover). If Parkio ever runs Redis Cluster/multiple
 * independent Redis masters, upgrade to the Redlock algorithm (e.g. via
 * Redisson) instead of this — this implementation does not attempt Redlock's
 * multi-node quorum.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:";

    // Only deletes the key if the value still matches our token — prevents
    // releasing a lock that has since expired and been re-acquired by someone else.
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end", Long.class);

    /**
     * Attempts to acquire the lock, returning a random token to prove
     * ownership on release, or {@code null} if someone else already holds it.
     */
    public String tryAcquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        String redisKey = LOCK_PREFIX + key;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, token, ttl);
            return Boolean.TRUE.equals(acquired) ? token : null;
        } catch (Exception ex) {
            // Fail closed: if Redis is down we cannot safely guarantee exclusivity,
            // so callers should treat a null return as "could not lock" and reject
            // the operation rather than proceeding unprotected.
            log.error("Redis lock acquisition failed for {}", key, ex);
            return null;
        }
    }

    /** Releases the lock — a no-op if this caller's token no longer matches (already expired/stolen). */
    public void release(String key, String token) {
        if (token == null) return;
        try {
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(LOCK_PREFIX + key), token);
        } catch (Exception ex) {
            log.error("Redis lock release failed for {} — it will expire naturally via TTL", key, ex);
        }
    }
}
