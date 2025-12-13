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

/**
 * Keycloak authentication filter - DISABLED
 * TODO: Implement proper Keycloak authentication when ready
 * Tech debt: @Component annotation commented out to disable filter registration
 */
// @Component - DISABLED: Commented out to prevent auth bypass security issue
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class KeycloakTokenFilter implements WebFilter {

  private final KeycloakTokenValidator validator;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // TODO: Keycloak validation temporarily disabled - implement later
    log.debug("Keycloak filter bypassed for path: {}", exchange.getRequest().getPath().value());
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
