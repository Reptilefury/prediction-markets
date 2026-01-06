package com.oregonmarkets.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class KeycloakTokenFilterTest {

  @Mock private KeycloakTokenValidator validator;

  @Mock private WebFilterChain chain;

  private ObjectMapper objectMapper;
  private KeycloakTokenFilter filter;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    filter = new KeycloakTokenFilter(validator, objectMapper);
  }

  @Test
  void filter_NonAdminPath_PassesThrough() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/register").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Filter is bypassed, so it just passes through
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    verify(chain).filter(exchange);
    verifyNoInteractions(validator);
  }

  @Test
  void filter_NonApiPath_PassesThrough() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/health").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    verify(chain).filter(exchange);
    verifyNoInteractions(validator);
  }

  @Test
  void filter_AdminPath_MissingAuthorization_ReturnsUnauthorized() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/admin/permissions").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verifyNoInteractions(validator);
  }

  @Test
  void filter_AdminPath_ValidAuthorizationToken_PassesThrough() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/admin/permissions")
            .header("Authorization", "Bearer valid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());
    when(validator.validate("valid-token")).thenReturn(Mono.just(Map.of("preferred_username", "admin")));

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    verify(validator).validate("valid-token");
    verify(chain).filter(exchange);
  }

  @Test
  void filter_AdminPath_InvalidAuthorizationToken_ReturnsUnauthorized() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/admin/permissions")
            .header("Authorization", "Bearer invalid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(validator.validate("invalid-token"))
        .thenReturn(Mono.error(new RuntimeException("boom")));

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(validator).validate("invalid-token");
  }

  @Test
  void filter_AdminPath_UsesXKeycloakTokenHeader() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/admin/permissions")
            .header("X-Keycloak-Token", "Bearer raw-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());
    when(validator.validate("raw-token")).thenReturn(Mono.just(Map.of("preferred_username", "admin")));

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    verify(validator).validate("raw-token");
    verify(chain).filter(exchange);
  }
}
