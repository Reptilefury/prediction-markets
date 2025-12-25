package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

class SecurityConfigTest {

  @Test
  void securityWebFilterChain_CreatesFilterChain() {
    SecurityConfig config = new SecurityConfig();
    ServerHttpSecurity http = ServerHttpSecurity.http();

    SecurityWebFilterChain chain = config.securityWebFilterChain(http);

    assertNotNull(chain);
  }

  @Test
  void corsConfigurationSource_CreatesSource() {
    SecurityConfig config = new SecurityConfig();

    CorsConfigurationSource source = config.corsConfigurationSource();

    assertNotNull(source);
  }
}
