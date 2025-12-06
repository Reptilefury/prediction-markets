package com.oregonMarkets.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.exception.KeycloakAuthException;
import com.oregonMarkets.common.exception.ResponseSerializationException;
import com.oregonMarkets.common.response.ApiResponse;
import com.oregonMarkets.common.response.ResponseCode;
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

    // Skip for registration endpoint
    if ("/api/auth/register".equals(path)) {
      return chain.filter(exchange);
    }

    if (path.startsWith("/api/")) {
      String keycloakHeader = exchange.getRequest().getHeaders().getFirst("X-Keycloak-Token");
      if (keycloakHeader == null || keycloakHeader.isBlank()) {
        return unauthorized(exchange, "KEYCLOAK_AUTH_FAILED", "Missing X-Keycloak-Token header");
      }
      String token =
          keycloakHeader.startsWith("Bearer ") ? keycloakHeader.substring(7) : keycloakHeader;
      return validator
          .validate(token)
          .flatMap(
              userInfo -> {
                exchange.getAttributes().put("keycloakUser", userInfo);
                return chain.filter(exchange);
              })
          .onErrorResume(
              e -> {
                // If Magic succeeded earlier in the chain, we should have magic context
                Object magicUserObj = exchange.getAttribute("magicUser");
                if (magicUserObj
                        instanceof
                        com.oregonMarkets.integration.magic.MagicClient.MagicUserInfo
                        magicUser
                    && magicUser.getEmail() != null) {
                  // Note: setPassword now requires accessToken, but we're in a filter context
                  // The proper flow is in UserRegistrationService which handles this during
                  // registration
                  log.debug(
                      "Keycloak validation failed for {}, but Magic context available",
                      magicUser.getEmail());
                }
                String msg =
                    e instanceof KeycloakAuthException
                        ? e.getMessage()
                        : "Keycloak token validation failed";
                return unauthorized(exchange, "KEYCLOAK_AUTH_FAILED", msg);
              });
    }

    return chain.filter(exchange);
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
}
