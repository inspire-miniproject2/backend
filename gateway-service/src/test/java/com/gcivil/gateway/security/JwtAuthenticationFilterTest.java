package com.gcivil.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcivil.gateway.config.GatewaySecurityProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "local-test-secret-that-is-at-least-32-bytes-long";

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            new GatewaySecurityProperties.JwtProperties(SECRET, "minwonon-auth", 30),
            new ObjectMapper());

    @Test
    void verifiesAccessTokenAndReplacesSpoofedInternalHeaders() throws Exception {
        String token = token("ACCESS", "ADMIN", Instant.now().plusSeconds(600));
        ServerWebExchange exchange = exchange("/api/v1/admin/statistics/daily", token)
                .mutate().request(request -> request.header(JwtAuthenticationFilter.USER_ID, "999"))
                .build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst(JwtAuthenticationFilter.USER_ID)).isEqualTo("101");
        assertThat(headers.getFirst(JwtAuthenticationFilter.LOGIN_ID)).isEqualTo("admin01");
        assertThat(headers.getFirst(JwtAuthenticationFilter.USER_ROLE)).isEqualTo("ADMIN");
        assertThat(headers.getFirst(JwtAuthenticationFilter.DEPARTMENT_ID)).isEqualTo("10");
        assertThat(headers.getFirst(JwtAuthenticationFilter.REQUEST_ID)).isNotBlank();
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }

    @Test
    void rejectsRequestWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notifications"));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("UNAUTHORIZED");
    }

    @Test
    void rejectsNonAdminFromStatisticsApi() throws Exception {
        MockServerWebExchange exchange = exchange(
                "/api/v1/admin/statistics/daily", token("ACCESS", "CITIZEN", Instant.now().plusSeconds(600)));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("FORBIDDEN");
    }

    @Test
    void rejectsExpiredTokenWithDedicatedCode() throws Exception {
        MockServerWebExchange exchange = exchange(
                "/api/v1/notifications", token("ACCESS", "CITIZEN", Instant.now().minusSeconds(60)));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("TOKEN_EXPIRED");
    }

    @Test
    void rejectsRefreshTokenFromApi() throws Exception {
        MockServerWebExchange exchange = exchange(
                "/api/v1/notifications", token("REFRESH", "CITIZEN", Instant.now().plusSeconds(600)));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("INVALID_TOKEN");
    }

    @Test
    void permitsPublicPathButRemovesSpoofedHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .header(JwtAuthenticationFilter.USER_ID, "999"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey(JwtAuthenticationFilter.USER_ID)).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(JwtAuthenticationFilter.REQUEST_ID)).isNotBlank();
    }

    @Test
    void doesNotPermitPathThatOnlyStartsLikePublicPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/public-responses-private"));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private MockServerWebExchange exchange(String path, String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String token(String tokenType, String role, Instant expiresAt) throws Exception {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(3600) : now.minusSeconds(1);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("101")
                .issuer("minwonon-auth")
                .claim("userId", 101L)
                .claim("loginId", "admin01")
                .claim("role", role)
                .claim("departmentId", 10L)
                .claim("tokenType", tokenType)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
