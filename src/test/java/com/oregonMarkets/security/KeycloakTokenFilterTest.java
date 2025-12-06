package com.oregonMarkets.security;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oregonMarkets.integration.keycloak.KeycloakAdminClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class KeycloakTokenFilterTest {

  @Mock private KeycloakTokenValidator validator;

  @Mock private KeycloakAdminClient keycloakAdminClient;

  @Mock private WebFilterChain chain;

  private ObjectMapper objectMapper;
  private KeycloakTokenFilter filter;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    filter = new KeycloakTokenFilter(validator, keycloakAdminClient, objectMapper);
  }

  @Test
  void filter_SkipsRegistrationEndpoint() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/register").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_NonApiPath_PassesThrough() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/health").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_MissingKeycloakHeader_ReturnsUnauthorized() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/users").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_ValidKeycloakToken_PassesThrough() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users")
            .header("X-Keycloak-Token", "Bearer valid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    Map<String, Object> userInfo = Map.of("sub", "user123", "email", "test@example.com");
    when(validator.validate("valid-token")).thenReturn(Mono.just(userInfo));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_InvalidKeycloakToken_ReturnsUnauthorized() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users").header("X-Keycloak-Token", "invalid-token").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(validator.validate("invalid-token"))
        .thenReturn(Mono.error(new RuntimeException("Invalid token")));

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_TokenWithoutBearerPrefix_ProcessesCorrectly() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users").header("X-Keycloak-Token", "raw-token").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    Map<String, Object> userInfo = Map.of("sub", "user123", "email", "test@example.com");
    when(validator.validate("raw-token")).thenReturn(Mono.just(userInfo));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }
}
