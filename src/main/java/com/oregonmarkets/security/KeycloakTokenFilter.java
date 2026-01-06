package com.oregonmarkets.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonmarkets.common.exception.KeycloakAuthException;
import com.oregonmarkets.common.exception.ResponseSerializationException;
import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Keycloak authentication filter for admin APIs
 * Validates Keycloak JWT tokens (from Azure AD/Keycloak login)
 * Handles /api/admin/* endpoints
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class KeycloakTokenFilter implements WebFilter {

  private final KeycloakTokenValidator validator;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    String method = exchange.getRequest().getMethod().name();
    log.info("KeycloakTokenFilter: Processing {} request for path: {}", method, path);

    // Only validate admin paths
    if (isAdminPath(path)) {
      log.info(
          "KeycloakTokenFilter: Admin path detected, validating Keycloak token for path: {}",
          path);
      return validateAndProceed(exchange, chain);
    }

    log.debug("KeycloakTokenFilter: Non-admin path, skipping Keycloak validation for path: {}", path);
    return chain.filter(exchange);
  }

  private Mono<Void> validateAndProceed(ServerWebExchange exchange, WebFilterChain chain) {
    String token = extractToken(exchange);
    String path = exchange.getRequest().getPath().value();
    log.info("KeycloakTokenFilter: Extracted token for path {}: {}", path, token != null ? "present" : "missing");

    if (token == null) {
      log.warn("KeycloakTokenFilter: Missing or invalid Authorization header for path: {}", path);
      return unauthorized(exchange, "UNAUTHORIZED", "Missing or invalid Authorization header");
    }

    log.info("KeycloakTokenFilter: Validating Keycloak JWT token for path: {}", path);
    return validator
        .validate(token)
        .flatMap(userInfo -> {
              log.info("KeycloakTokenFilter: Keycloak JWT validation successful for user: {} on path: {}", userInfo.get("preferred_username"), path);
              exchange.getAttributes().put("keycloakUser", userInfo);
              exchange.getAttributes().put("keycloakToken", token);
              return chain.filter(exchange);
            })
        .onErrorResume(e -> {
              log.error("KeycloakTokenFilter: Keycloak JWT validation failed for path {}: {}", path, e.getMessage(), e);
              return unauthorized(exchange, "INVALID_TOKEN", getErrorMessage(e));
            });
  }

  private String extractToken(ServerWebExchange exchange) {
    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
    if (authHeader == null) {
      authHeader = exchange.getRequest().getHeaders().getFirst("X-Keycloak-Token");
    }
    log.debug("Authorization header present: {}", authHeader != null);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring(7);
    log.debug("Extracted JWT token length: {}", token.length());
    return token;
  }

  private String getErrorMessage(Throwable e) {
    return e instanceof KeycloakAuthException ? e.getMessage() : "Keycloak token validation failed";
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    ApiResponse<Void> errorResponse = ApiResponse.error(ResponseCode.UNAUTHORIZED, message);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
      return exchange
          .getResponse()
          .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize error response", e);
      throw new ResponseSerializationException("Keycloak error response serialization failed", e);
    }
  }

  private boolean isAdminPath(String path) {
    return path.startsWith("/api/admin")
        || path.startsWith("/api/v1/admin")
        || path.startsWith("/prediction-markets/api/admin")
        || path.startsWith("/prediction-markets/api/v1/admin");
  }
}
