package com.oregonmarkets.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DataMaskingUtilTest {

  @Test
  void maskEmail_ValidEmail() {
    // When
    String result = DataMaskingUtil.maskEmail("user@example.com");

    // Then: first 2 chars + asterisks for remaining local part + domain
    assertThat(result).isEqualTo("us**@example.com");
  }

  @Test
  void maskEmail_EmailWithLongLocalPart() {
    // When
    String result = DataMaskingUtil.maskEmail("longemail@example.com");

    // Then: first 2 chars + asterisks for remaining local part + domain
    assertThat(result).isEqualTo("lo*******@example.com");
  }

  @Test
  void maskEmail_EmailWithoutAt() {
    // When
    String result = DataMaskingUtil.maskEmail("invalidemail");

    // Then: first 2 chars + asterisks for remainder
    assertThat(result).isEqualTo("in**********");
  }

  @Test
  void maskEmail_EmptyString() {
    // When
    String result = DataMaskingUtil.maskEmail("");

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskEmail_NullValue() {
    // When
    String result = DataMaskingUtil.maskEmail(null);

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskEmail_ShortLocalPart() {
    // When
    String result = DataMaskingUtil.maskEmail("a@example.com");

    // Then
    assertThat(result).contains("@example.com");
  }

  @Test
  void maskWalletAddress_StandardEthereumAddress() {
    // When
    String result = DataMaskingUtil.maskWalletAddress("0x1234567890abcdef1234567890abcdef12345678");

    // Then
    assertThat(result).isEqualTo("0x1234...5678");
  }

  @Test
  void maskWalletAddress_ShortAddress() {
    // When
    String result = DataMaskingUtil.maskWalletAddress("0x1234");

    // Then
    assertThat(result).contains("*");
  }

  @Test
  void maskWalletAddress_EmptyString() {
    // When
    String result = DataMaskingUtil.maskWalletAddress("");

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskWalletAddress_NullValue() {
    // When
    String result = DataMaskingUtil.maskWalletAddress(null);

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskWalletAddress_ExactlyTenCharacters() {
    // When
    String result = DataMaskingUtil.maskWalletAddress("0x12345678");

    // Then
    assertThat(result).hasSize(10);
  }

  @Test
  void maskUserId_StandardUUID() {
    // When
    String result = DataMaskingUtil.maskUserId("550e8400-e29b-41d4-a716-446655440000");

    // Then
    assertThat(result).isEqualTo("550e8400-****");
  }

  @Test
  void maskUserId_ShortId() {
    // When
    String result = DataMaskingUtil.maskUserId("12345");

    // Then
    assertThat(result).contains("*");
  }

  @Test
  void maskUserId_EmptyString() {
    // When
    String result = DataMaskingUtil.maskUserId("");

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskUserId_NullValue() {
    // When
    String result = DataMaskingUtil.maskUserId(null);

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskUserId_ExactlyEightCharacters() {
    // When
    String result = DataMaskingUtil.maskUserId("12345678");

    // Then: length is exactly 8, so falls into else branch -> first 4 chars only
    assertThat(result).isEqualTo("1234****");
  }

  @Test
  void maskString_WithPrefixAndSuffix() {
    // When
    String result = DataMaskingUtil.maskString("0x1234567890abcdef", 4, 4);

    // Then: first 4 + (16-4-4=10) asterisks + last 4
    assertThat(result).isEqualTo("0x12**********cdef");
  }

  @Test
  void maskString_WithPrefixOnly() {
    // When
    String result = DataMaskingUtil.maskString("secretpassword123", 4, 0);

    // Then
    assertThat(result).isEqualTo("secr*************");
  }

  @Test
  void maskString_WithSuffixOnly() {
    // When
    String result = DataMaskingUtil.maskString("secretpassword123", 0, 3);

    // Then
    assertThat(result).isEqualTo("**************123");
  }

  @Test
  void maskString_LongerThanPrefixAndSuffix() {
    // When
    String result = DataMaskingUtil.maskString("0123456789", 2, 2);

    // Then: first 2 + (10-2-2=6) asterisks + last 2
    assertThat(result).isEqualTo("01******89");
  }

  @Test
  void maskString_ShorterThanPrefixAndSuffix() {
    // When
    String result = DataMaskingUtil.maskString("12", 4, 4);

    // Then
    assertThat(result).isEqualTo("**");
  }

  @Test
  void maskString_SingleCharacter() {
    // When
    String result = DataMaskingUtil.maskString("a", 4, 4);

    // Then
    assertThat(result).isEqualTo("*");
  }

  @Test
  void maskString_EmptyString() {
    // When
    String result = DataMaskingUtil.maskString("", 2, 2);

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskString_NullValue() {
    // When
    String result = DataMaskingUtil.maskString(null, 2, 2);

    // Then
    assertThat(result).isEqualTo("[empty]");
  }

  @Test
  void maskString_ZeroPrefixAndSuffix() {
    // When
    String result = DataMaskingUtil.maskString("secret", 0, 0);

    // Then
    assertThat(result).isEqualTo("******");
  }

  @Test
  void sanitizeErrorBody_LongErrorMessage() {
    // Given
    String longError = "a".repeat(300);

    // When
    String result = DataMaskingUtil.sanitizeErrorBody(longError);

    // Then: truncates to 200 chars + " ... [truncated]"
    assertThat(result).isEqualTo("a".repeat(200) + "... [truncated]");
  }

  @Test
  void sanitizeErrorBody_ShortErrorMessage() {
    // Given
    String shortError = "Error: Something went wrong";

    // When
    String result = DataMaskingUtil.sanitizeErrorBody(shortError);

    // Then
    assertThat(result).isEqualTo(shortError);
  }

  @Test
  void sanitizeErrorBody_EmptyString() {
    // When
    String result = DataMaskingUtil.sanitizeErrorBody("");

    // Then
    assertThat(result).isEqualTo("[empty response]");
  }

  @Test
  void sanitizeErrorBody_NullValue() {
    // When
    String result = DataMaskingUtil.sanitizeErrorBody(null);

    // Then
    assertThat(result).isEqualTo("[empty response]");
  }

  @Test
  void sanitizeErrorBody_ExactlyOneHundredCharacters() {
    // Given
    String error = "a".repeat(100);

    // When
    String result = DataMaskingUtil.sanitizeErrorBody(error);

    // Then
    assertThat(result).isEqualTo(error);
  }
}


