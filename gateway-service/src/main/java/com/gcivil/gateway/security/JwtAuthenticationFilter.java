package com.gcivil.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcivil.gateway.config.GatewaySecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@EnableConfigurationProperties(GatewaySecurityProperties.JwtProperties.class)
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    static final String USER_ID = "X-User-Id";
    static final String LOGIN_ID = "X-Login-Id";
    static final String USER_ROLE = "X-User-Role";
    static final String DEPARTMENT_ID = "X-Department-Id";
    static final String REQUEST_ID = "X-Request-Id";

    private static final Set<String> ROLES = Set.of("CITIZEN", "OFFICER", "ADMIN");
    private static final List<String> INTERNAL_HEADERS = List.of(
            USER_ID, LOGIN_ID, USER_ROLE, DEPARTMENT_ID, REQUEST_ID);

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(GatewaySecurityProperties.JwtProperties properties, ObjectMapper objectMapper) {
        byte[] secret = requireSecret(properties.secret());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(
                Duration.ofSeconds(properties.clockSkewSeconds()));
        OAuth2TokenValidator<Jwt> claimsValidator = this::validateRequiredClaims;
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtIssuerValidator(properties.issuer()),
                new JwtClaimValidator<>("tokenType", "ACCESS"::equals),
                claimsValidator));
        this.jwtDecoder = decoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        ServerWebExchange sanitized = exchange.mutate().request(request -> request.headers(headers -> {
            INTERNAL_HEADERS.forEach(headers::remove);
            headers.set(REQUEST_ID, requestId);
        })).build();
        sanitized.getResponse().getHeaders().set(REQUEST_ID, requestId);

        String path = sanitized.getRequest().getPath().value();
        if (isPublic(path) || HttpMethod.OPTIONS.equals(sanitized.getRequest().getMethod())) {
            return chain.filter(sanitized);
        }

        String authorization = sanitized.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() == 7) {
            return error(sanitized, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(7));
            String role = jwt.getClaimAsString("role");
            if (path.startsWith("/api/v1/admin/statistics") && !"ADMIN".equals(role)) {
                return error(sanitized, HttpStatus.FORBIDDEN, "FORBIDDEN", "관리자 권한이 필요합니다.");
            }
            ServerWebExchange authenticated = sanitized.mutate().request(request -> request.headers(headers -> {
                headers.remove(HttpHeaders.AUTHORIZATION);
                headers.set(USER_ID, String.valueOf(((Number) jwt.getClaim("userId")).longValue()));
                headers.set(LOGIN_ID, jwt.getClaimAsString("loginId"));
                headers.set(USER_ROLE, role);
                Number departmentId = jwt.getClaim("departmentId");
                if (departmentId != null) {
                    headers.set(DEPARTMENT_ID, String.valueOf(departmentId.longValue()));
                }
            })).build();
            return chain.filter(authenticated);
        } catch (JwtValidationException exception) {
            boolean expired = exception.getErrors().stream()
                    .map(OAuth2Error::getDescription)
                    .filter(description -> description != null)
                    .anyMatch(description -> description.toLowerCase().contains("expir"));
            return error(sanitized, HttpStatus.UNAUTHORIZED,
                    expired ? "TOKEN_EXPIRED" : "INVALID_TOKEN",
                    expired ? "만료된 토큰입니다." : "유효하지 않은 토큰입니다.");
        } catch (RuntimeException exception) {
            return error(sanitized, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
    }

    private OAuth2TokenValidatorResult validateRequiredClaims(Jwt jwt) {
        Object userId = jwt.getClaim("userId");
        String loginId = jwt.getClaimAsString("loginId");
        String role = jwt.getClaimAsString("role");
        boolean valid = userId instanceof Number
                && String.valueOf(((Number) userId).longValue()).equals(jwt.getSubject())
                && loginId != null && !loginId.isBlank()
                && ROLES.contains(role);
        return valid ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Required access token claims are invalid", null));
    }

    private boolean isPublic(String path) {
        return path.equals("/api/v1/auth/signup")
                || path.equals("/api/v1/auth/login")
                || isPathOrChild(path, "/api/v1/complaint-categories")
                || isPathOrChild(path, "/api/v1/public-responses")
                || path.equals("/actuator/health")
                || path.equals("/api/v1/complaints/ping")
                || path.equals("/api/v1/notifications/ping")
                || path.equals("/api/v1/admin/statistics/ping");
    }

    private boolean isPathOrChild(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private byte[] requireSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 UTF-8 bytes");
        }
        return bytes;
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", code);
            error.put("message", message);
            error.put("details", null);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("error", error);
            body.put("requestId", exchange.getResponse().getHeaders().getFirst(REQUEST_ID));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException exception) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
