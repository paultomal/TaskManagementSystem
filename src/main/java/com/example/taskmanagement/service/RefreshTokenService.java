package com.example.taskmanagement.service;

import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

/**
 * Redis-backed whitelist of valid refresh-token JTIs so that logout and
 * admin-triggered force-logout can revoke tokens even though the JWTs
 * themselves are stateless.
 *
 * <p>Each valid token is stored as a key {@code refresh:{username}:{jti}} with a
 * per-key TTL matching the refresh-token lifetime, so expired entries are
 * evicted by Redis automatically. Using Redis (instead of an in-memory map)
 * means the whitelist survives app restarts and is shared across every instance
 * of the app — required for a clustered/persistent deployment.
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final String VALUE = "1";

    private final StringRedisTemplate redis;

    public RefreshTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void store(String username, String jti, long ttlMs) {
        redis.opsForValue().set(key(username, jti), VALUE, Duration.ofMillis(ttlMs));
    }

    public boolean isValid(String username, String jti) {
        return Boolean.TRUE.equals(redis.hasKey(key(username, jti)));
    }

    public void revoke(String username, String jti) {
        redis.delete(key(username, jti));
    }

    /**
     * Force-logout: revoke every refresh token for the user by scanning for all
     * keys under the user's prefix and deleting them.
     */
    public void revokeAll(String username) {
        String pattern = KEY_PREFIX + username + ":*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            Set<String> batch = new java.util.HashSet<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
            }
            if (!batch.isEmpty()) {
                redis.delete(batch);
            }
        }
    }

    private String key(String username, String jti) {
        return KEY_PREFIX + username + ":" + jti;
    }
}
