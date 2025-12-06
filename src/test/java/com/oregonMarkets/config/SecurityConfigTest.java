package com.oregonMarkets.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void securityWebFilterChain_ShouldReturnValidChain() {
        SecurityConfig config = new SecurityConfig();
        SecurityWebFilterChain chain = config.securityWebFilterChain(ServerHttpSecurity.http());
        assertNotNull(chain);
    }

    @Test
    void corsConfigurationSource_ShouldReturnValidSource() {
        SecurityConfig config = new SecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();
        assertNotNull(source);
    }
}