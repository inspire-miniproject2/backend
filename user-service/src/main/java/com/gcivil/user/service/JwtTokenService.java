package com.gcivil.user.service;

import com.gcivil.user.config.AuthProperties;
import com.gcivil.user.domain.User;
import com.gcivil.user.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final AuthProperties authProperties;
    private final SecretKey secretKey;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        byte[] keyBytes = authProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(authProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(authProperties.getAccessTokenTtlSeconds())))
                .claim("userId", user.getId())
                .claim("loginId", user.getLoginId())
                .claim("role", user.getRole().name())
                .claim("departmentId", user.getDepartmentId())
                .claim("tokenType", "ACCESS")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(authProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(authProperties.getRefreshTokenTtlSeconds())))
                .claim("userId", user.getId())
                .claim("tokenType", "REFRESH")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidateAccessToken(String token) {
        return parseAndValidateToken(token, "ACCESS", "Access Token만 사용할 수 있습니다.");
    }

    public Claims parseAndValidateRefreshToken(String token) {
        return parseAndValidateToken(token, "REFRESH", "Refresh Token만 사용할 수 있습니다.");
    }

    private Claims parseAndValidateToken(String token, String expectedTokenType, String message) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(authProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedTokenType.equals(claims.get("tokenType"))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", message);
            }
            return claims;
        } catch (ApiException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
    }
}
