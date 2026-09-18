package com.example.taskmanagement.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * In-memory whitelist of valid refresh-token JTIs so that logout and
 * admin-triggered force-logout can revoke tokens even though the JWTs
 * themselves are stateless.
 *
 * <p>Keyed by {@code {username}:{jti}} with an absolute expiry timestamp;
 * expired entries are treated as invalid and cleaned up lazily. This keeps the
 * app runnable without an external Redis server. Note that the whitelist is not
 * shared across instances and is lost on restart — swap the implementation back
 * to a Redis-backed store for a clustered/persistent deployment.
 */
@Service
public class RefreshTokenService {

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public void store(String username, String jti, long ttlMs) {
        tokens.put(key(username, jti), System.currentTimeMillis() + ttlMs);
    }

    public boolean isValid(String username, String jti) {
        Long expiry = tokens.get(key(username, jti));
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            tokens.remove(key(username, jti));
            return false;
        }
        return true;
    }

    public void revoke(String username, String jti) {
        tokens.remove(key(username, jti));
    }

    /**
     * Force-logout: revoke every refresh token for the user.
     */
    public void revokeAll(String username) {
        String prefix = username + ":";
        tokens.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private String key(String username, String jti) {
        return username + ":" + jti;
    }
}
