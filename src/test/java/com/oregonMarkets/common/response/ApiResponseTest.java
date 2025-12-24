package com.oregonMarkets.common.response;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ApiResponseTest {

  @Test
  void success_WithDataOnly() {
    // Given
    String testData = "test data";

    // When
    ApiResponse<String> response = ApiResponse.success(testData);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
    assertThat(response.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
    assertThat(response.getData()).isEqualTo(testData);
    assertThat(response.getError()).isNull();
  }

  @Test
  void success_WithResponseCodeAndData() {
    // Given
    String testData = "test data";
    ResponseCode responseCode = ResponseCode.CREATED;

    // When
    ApiResponse<String> response = ApiResponse.success(responseCode, testData);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
    assertThat(response.getMessage()).isEqualTo(responseCode.getMessage());
    assertThat(response.getCode()).isEqualTo(responseCode.getCode());
    assertThat(response.getData()).isEqualTo(testData);
  }

  @Test
  void success_WithCustomMessage() {
    // Given
    String testData = "test data";
    String customMessage = "Custom success message";
    ResponseCode responseCode = ResponseCode.SUCCESS;

    // When
    ApiResponse<String> response = ApiResponse.success(responseCode, customMessage, testData);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
    assertThat(response.getMessage()).isEqualTo(customMessage);
    assertThat(response.getCode()).isEqualTo(responseCode.getCode());
    assertThat(response.getData()).isEqualTo(testData);
  }

  @Test
  void error_WithResponseCode() {
    // Given
    ResponseCode responseCode = ResponseCode.UNAUTHORIZED;

    // When
    ApiResponse<Void> response = ApiResponse.error(responseCode);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.FAILED);
    assertThat(response.getMessage()).isEqualTo(responseCode.getMessage());
    assertThat(response.getCode()).isEqualTo(responseCode.getCode());
    assertThat(response.getError()).isNotNull();
    assertThat(response.getError().getCode()).isEqualTo(responseCode.getCode());
    assertThat(response.getError().getMessage()).isEqualTo(responseCode.getMessage());
    assertThat(response.getData()).isNull();
  }

  @Test
  void error_WithCustomMessage() {
    // Given
    ResponseCode responseCode = ResponseCode.UNAUTHORIZED;
    String customMessage = "Custom error message";

    // When
    ApiResponse<Void> response = ApiResponse.error(responseCode, customMessage);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.FAILED);
    assertThat(response.getMessage()).isEqualTo(customMessage);
    assertThat(response.getCode()).isEqualTo(responseCode.getCode());
    assertThat(response.getError()).isNotNull();
    assertThat(response.getError().getMessage()).isEqualTo(customMessage);
  }

  @Test
  void error_WithDetails() {
    // Given
    ResponseCode responseCode = ResponseCode.VALIDATION_ERROR;
    String customMessage = "Validation failed message";
    String details = "Invalid input parameters";

    // When
    ApiResponse<Void> response = ApiResponse.error(responseCode, customMessage, details);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.FAILED);
    assertThat(response.getError()).isNotNull();
    assertThat(response.getError().getDetails()).isEqualTo(details);
  }

  @Test
  void validationError() {
    // Given
    Map<String, String> validationErrors = new HashMap<>();
    validationErrors.put("email", "Email is invalid");
    validationErrors.put("password", "Password is too short");

    // When
    ApiResponse<Void> response = ApiResponse.validationError(validationErrors);

    // Then
    assertThat(response.getStatus()).isEqualTo(ResponseStatus.FAILED);
    assertThat(response.getCode()).isEqualTo(ResponseCode.VALIDATION_ERROR.getCode());
    assertThat(response.getError()).isNotNull();
    assertThat(response.getError().getValidationErrors()).containsAllEntriesOf(validationErrors);
  }

  @Test
  void withMetadata_SingleEntry() {
    // Given
    ApiResponse<String> response = ApiResponse.success("test");
    String key = "page";
    Object value = 1;

    // When
    ApiResponse<String> result = response.withMetadata(key, value);

    // Then
    assertThat(result).isSameAs(response);
    assertThat(response.getMetadata()).isNotNull();
    assertThat(response.getMetadata()).containsEntry(key, value);
  }

  @Test
  void withMetadata_MultipleEntries() {
    // Given
    ApiResponse<String> response = ApiResponse.success("test");
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("total", 100);
    metadata.put("page", 1);

    // When
    ApiResponse<String> result = response.withMetadata(metadata);

    // Then
    assertThat(result).isSameAs(response);
    assertThat(response.getMetadata()).containsAllEntriesOf(metadata);
  }

  @Test
  void getMetadata_DefensiveCopy() {
    // Given
    ApiResponse<String> response = ApiResponse.success("test");
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("key", "value");
    response.setMetadata(metadata);

    // When
    Map<String, Object> retrieved = response.getMetadata();
    retrieved.put("newKey", "newValue");

    // Then
    assertThat(response.getMetadata()).doesNotContainKey("newKey");
  }

  @Test
  void setMetadata_DefensiveCopy() {
    // Given
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("key", "value");

    // When
    ApiResponse<String> response = ApiResponse.success("test");
    response.setMetadata(metadata);
    metadata.put("newKey", "newValue");

    // Then
    assertThat(response.getMetadata()).doesNotContainKey("newKey");
  }

  @Test
  void timestamp_Default() {
    // When
    ApiResponse<Void> response = ApiResponse.error(ResponseCode.UNAUTHORIZED);

    // Then
    assertThat(response.getTimestamp()).isNotNull();
  }
}

