package com.oregonmarkets.integration.magic;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonmarkets.common.exception.MagicAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

class MagicDIDValidatorTest {

  private MagicDIDValidator validator;

  @BeforeEach
  void setUp() {
    validator = new MagicDIDValidator();
    ReflectionTestUtils.setField(validator, "magicApiKey", "test-key");
  }

  @Test
  void validateDIDToken_NullToken_ThrowsException() {
    StepVerifier.create(validator.validateDIDToken(null))
        .expectError(MagicAuthException.class)
        .verify();
  }

  @Test
  void validateDIDToken_EmptyToken_ThrowsException() {
    StepVerifier.create(validator.validateDIDToken(""))
        .expectError(MagicAuthException.class)
        .verify();
  }

  @Test
  void validateDIDToken_InvalidBase64_ThrowsException() {
    StepVerifier.create(validator.validateDIDToken("invalid-base64!@#"))
        .expectError(MagicAuthException.class)
        .verify();
  }

  @Test
  void validateDIDToken_InvalidJson_ThrowsException() {
    String invalidJson = java.util.Base64.getEncoder().encodeToString("invalid-json".getBytes());
    StepVerifier.create(validator.validateDIDToken(invalidJson))
        .expectError(MagicAuthException.class)
        .verify();
  }

  @Test
  void validateDIDToken_SingleElement_ThrowsException() {
    String singleElement =
        java.util.Base64.getEncoder().encodeToString("[\"only-one-element\"]".getBytes());
    StepVerifier.create(validator.validateDIDToken(singleElement))
        .expectError(MagicAuthException.class)
        .verify();
  }

  @Test
  void validateDIDToken_MissingClaims_ThrowsException() {
    String missingClaims =
        java.util.Base64.getEncoder()
            .encodeToString("[\"proof\",\"{\\\"sub\\\":\\\"user123\\\"}\"]".getBytes());
    StepVerifier.create(validator.validateDIDToken(missingClaims))
        .expectError(MagicAuthException.class)
        .verify();
  }
}
