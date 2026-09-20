package com.example.parkio.security;

/**
 * Contract for token revocation stores.
 */
public interface TokenBlacklist {

    void revoke(String token, java.util.Date expiry);

    boolean isBlacklisted(String token);
}
