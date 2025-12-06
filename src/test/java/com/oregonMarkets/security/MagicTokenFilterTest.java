package com.oregonMarkets.security;

import static org.mockito.Mockito.when;

import com.oregonMarkets.common.exception.MagicAuthException;
import com.oregonMarkets.dto.ErrorType;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
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
class MagicTokenFilterTest {

  @Mock private MagicDIDValidator magicValidator;

  @Mock private ErrorResponseBuilder errorResponseBuilder;

  @Mock private WebFilterChain chain;

  private MagicTokenFilter filter;

  @BeforeEach
  void setUp() {
    filter = new MagicTokenFilter(magicValidator, errorResponseBuilder);
  }

  @Test
  void filter_NonApiPath_PassesThrough() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/health").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_ApiPath_ValidatesToken() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users")
            .header("Authorization", "Bearer valid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    MagicDIDValidator.MagicUserInfo userInfo =
        new MagicDIDValidator.MagicUserInfo(
            "did:ethr:0x123", "test@example.com", "0x123", null, "user123");
    when(magicValidator.validateDIDToken("valid-token")).thenReturn(Mono.just(userInfo));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_PredictionMarketsApiPath_ValidatesToken() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/prediction-markets/api/users")
            .header("Authorization", "Bearer valid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    MagicDIDValidator.MagicUserInfo userInfo =
        new MagicDIDValidator.MagicUserInfo(
            "did:ethr:0x123", "test@example.com", "0x123", null, "user123");
    when(magicValidator.validateDIDToken("valid-token")).thenReturn(Mono.just(userInfo));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_MissingAuthorizationHeader_ReturnsError() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/users").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(errorResponseBuilder.buildErrorResponse(
            ErrorType.MAGIC_AUTH_FAILED, "Missing or invalid Authorization header"))
        .thenReturn("error response".getBytes());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_InvalidAuthorizationHeader_ReturnsError() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users").header("Authorization", "Invalid header").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(errorResponseBuilder.buildErrorResponse(
            ErrorType.MAGIC_AUTH_FAILED, "Missing or invalid Authorization header"))
        .thenReturn("error response".getBytes());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_TokenValidationFails_ReturnsError() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users")
            .header("Authorization", "Bearer invalid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(magicValidator.validateDIDToken("invalid-token"))
        .thenReturn(Mono.error(new RuntimeException("Validation failed")));
    when(errorResponseBuilder.buildErrorResponse(
            ErrorType.MAGIC_AUTH_FAILED, "Magic token validation failed"))
        .thenReturn("validation error".getBytes());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_MagicAuthException_ReturnsSpecificError() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/users")
            .header("Authorization", "Bearer invalid-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(magicValidator.validateDIDToken("invalid-token"))
        .thenReturn(Mono.error(new MagicAuthException("Magic auth failed")));
    when(errorResponseBuilder.buildErrorResponse(ErrorType.MAGIC_AUTH_FAILED, "Magic auth failed"))
        .thenReturn("magic error".getBytes());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }
}
