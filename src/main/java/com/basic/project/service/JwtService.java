package com.basic.project.service;

import com.basic.project.exception.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    private final Key accessToken;
    private final Key refreshToken;
    private final long expiresIn;
    private final long refreshExpiresIn;


    public JwtService(
            @Value("${jwt.access.secret}") String accessToken,
            @Value("${jwt.refresh.secret}") String refreshToken,
            @Value("${jwt.access.ttl}") long expiresIn,
            @Value("${jwt.refresh.ttl}") long refreshExpiresIn
    ) {

        validateSecret(accessToken, "Access token secret");
        validateSecret(refreshToken, "Refresh token secret");

        this.accessToken = Keys.hmacShaKeyFor(accessToken.getBytes(StandardCharsets.UTF_8));
        this.refreshToken = Keys.hmacShaKeyFor(refreshToken.getBytes(StandardCharsets.UTF_8));
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;

        log.info("JWT access token: {}s, refreshToken: {}s" , accessToken, refreshToken);
    }

    /**
     * Генерация accesss токена
     */
    public String generateAccessToken(String subject) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(expiresIn);
            String token =Jwts.builder()
                    .setSubject(subject)
                    .addClaims(Map.of(
                            "type", "access"
                    ))
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiresAt))
                    .signWith(accessToken, SignatureAlgorithm.HS512)
                    .compact();
            return token;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to generate access token",e);
        }
    }

    /**
     * Генерация refresh токена
     */
    public String generateRefreshToken(String subject) {

        try {

            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(refreshExpiresIn);
            String token = Jwts.builder()
                    .setSubject(subject)
                    .addClaims(Map.of(
                            "type", "refresh"
                    ))
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiresAt))
                    .signWith(refreshToken, SignatureAlgorithm.HS512)
                    .compact();
            return token;
        }

        catch (Exception e) {
            throw new RuntimeException("Failed to generate refresh token",e);
        }
    }

    /**
     *AccessToken парсинг
     */
    public Jws<Claims> parseAccess(String token){
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(accessToken)
                    .build()
                    .parseClaimsJws(token);

            String tokenType = claims.getBody().get("type", String.class);
            if (!tokenType.equals("access")) {
                throw new RuntimeException("Invalid access token");
            }
            return claims;

        }
        catch (ExpiredJwtException e) {
            log.warn("Expired access token: {}", e.getMessage());
            throw AuthException.invalidInput("Token expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw AuthException.invalidInput("Unsupported token format");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw AuthException.invalidInput("Malformed token");
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw AuthException.invalidInput("Invalid token signature");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw AuthException.invalidInput("Invalid token");
        }
    }

    /**
     * Парсинг refresh токена
     */
    public Jws<Claims> parseRefresh(String token){
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(refreshToken)
                    .build()
                    .parseClaimsJws(token);

            String tokenType = claims.getBody().get("type", String.class);
            if (!tokenType.equals("refresh")) {
                throw new RuntimeException("Invalid refresh token");
            }
            return claims;
        }
        catch (ExpiredJwtException e) {
            log.warn("Expired refresh token: {}", e.getMessage());
            throw AuthException.invalidInput("Token expired");
        }
        catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw AuthException.invalidInput("Unsupported token format");
        }
        catch (MalformedJwtException e) {
            log.warn("Malformed refresh token: {}", e.getMessage());
            throw AuthException.invalidInput("Malformed token");
        }
        catch (SecurityException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw AuthException.invalidInput("Invalid refresh token");
        }
        catch (IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw AuthException.invalidInput("Invalid refresh token");
        }
    }

    /**
     * Валидация секрета
     */

    public void validateSecret(String secret, String secretName) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException(secretName + " cannot be null or empty");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException(secretName + " must be at least 32 characters long");
        }

        if (secret.contains("your-super-secret") && !secret.contains("development-only")) {
            throw new IllegalArgumentException(secretName + " contains default value - change it in production!");
        }
    }

    /**
     * Проверка валидности токена без выброса исключения
     */
    public boolean isValidAccessToken(String token) {
        try {
            parseAccess(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Получение email из токена
     */
    public String getEmailFromToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(accessToken)
                    .build()
                    .parseClaimsJws(token);

            return claims.getBody().getSubject();
        } catch (Exception e) {
            log.error("Error extracting email from token", e);
            throw new AuthException(AuthException.ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
