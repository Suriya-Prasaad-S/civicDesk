package com.civicdesk.permit.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) { log.error("Invalid JWT: {}", e.getMessage()); }
        catch (ExpiredJwtException e)    { log.error("JWT expired: {}", e.getMessage()); }
        catch (UnsupportedJwtException e){ log.error("Unsupported JWT: {}", e.getMessage()); }
        catch (IllegalArgumentException e){ log.error("JWT empty: {}", e.getMessage()); }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public String getEmailFromToken(String token)  { return getClaims(token).get("userId", String.class); }
    public String getRoleFromToken(String token)    { return getClaims(token).get("role", String.class); }
    public Long   getUserIdFromToken(String token)  { return Long.parseLong(getClaims(token).get("userId", String.class)); }
}
