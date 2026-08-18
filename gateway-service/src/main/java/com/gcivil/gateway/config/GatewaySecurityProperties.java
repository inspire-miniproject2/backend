package com.gcivil.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

public final class GatewaySecurityProperties {
    private GatewaySecurityProperties() {
    }

    @ConfigurationProperties(prefix = "jwt")
    public record JwtProperties(String secret, String issuer, long clockSkewSeconds) {
        public JwtProperties {
            issuer = issuer == null || issuer.isBlank() ? "minwonon-auth" : issuer;
            clockSkewSeconds = clockSkewSeconds <= 0 ? 30 : clockSkewSeconds;
        }
    }

    @ConfigurationProperties(prefix = "gcivil.cors")
    public record CorsProperties(List<String> allowedOrigins) {
        public CorsProperties {
            allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                    ? List.of("http://localhost:5173") : List.copyOf(allowedOrigins);
        }
    }
}
