package com.gcivil.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorsConfigTest {
    private final CorsWebFilter filter = new GatewayCorsConfig().corsWebFilter(
            new GatewaySecurityProperties.CorsProperties(List.of("http://localhost:5173")));

    @Test
    void allowsConfiguredFrontendPreflight() {
        MockServerWebExchange exchange = preflight("http://localhost:5173");

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://localhost:5173");
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowMethods())
                .contains(HttpMethod.GET);
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowCredentials()).isNotEqualTo(true);
    }

    @Test
    void rejectsUnknownOriginPreflight() {
        MockServerWebExchange exchange = preflight("http://malicious.example");

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    private MockServerWebExchange preflight(String origin) {
        return MockServerWebExchange.from(MockServerHttpRequest.options("http://localhost:8080/api/v1/notifications")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));
    }
}
