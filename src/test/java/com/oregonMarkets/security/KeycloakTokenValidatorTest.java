package com.oregonMarkets.security;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonMarkets.common.exception.KeycloakAuthException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class KeycloakTokenValidatorTest {

  private KeycloakTokenValidator validator;

  @BeforeEach
  void setUp() {
    validator = new KeycloakTokenValidator();
  }

  @Test
  void validate_WithNullToken_ShouldReturnError() {
    StepVerifier.create(validator.validate(null)).expectError(KeycloakAuthException.class).verify();
  }

  @Test
  void validate_WithBlankToken_ShouldReturnError() {
    StepVerifier.create(validator.validate("")).expectError(KeycloakAuthException.class).verify();
  }

  @Test
  void validate_WithInvalidFormat_ShouldReturnError() {
    StepVerifier.create(validator.validate("invalid"))
        .expectError(KeycloakAuthException.class)
        .verify();
  }

  @Test
  void validate_WithValidToken_ShouldReturnUserInfo() {
    String payload =
        "{\"sub\":\"123\",\"email\":\"test@example.com\",\"preferred_username\":\"testuser\"}";
    String encodedPayload =
        Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = "header." + encodedPayload + ".signature";

    StepVerifier.create(validator.validate(token))
        .assertNext(
            userInfo -> {
              assertEquals("123", userInfo.get("sub"));
              assertEquals("test@example.com", userInfo.get("email"));
              assertEquals("testuser", userInfo.get("preferred_username"));
            })
        .verifyComplete();
  }

  @Test
  void validate_WithExpiredToken_ShouldReturnError() {
    long expiredTime = Instant.now().minusSeconds(3600).getEpochSecond();
    String payload = "{\"sub\":\"123\",\"exp\":" + expiredTime + "}";
    String encodedPayload =
        Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = "header." + encodedPayload + ".signature";

    StepVerifier.create(validator.validate(token))
        .expectError(KeycloakAuthException.class)
        .verify();
  }
}
