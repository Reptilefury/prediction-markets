package com.oregonMarkets.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.exception.ResponseSerializationException;
import com.oregonMarkets.common.response.ApiResponse;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.dto.ErrorResponse;
import com.oregonMarkets.dto.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorResponseBuilderTest {

  @Mock
  private ObjectMapper objectMapper;

  private ErrorResponseBuilder errorResponseBuilder;

  @BeforeEach
  void setUp() {
    errorResponseBuilder = new ErrorResponseBuilder(objectMapper);
  }

  @Test
  void buildErrorResponse_Success() {
    try {
      // Given
      ErrorType errorType = ErrorType.KEYCLOAK_AUTH_FAILED;
      String message = "Token validation failed";
      byte[] expectedBytes = new byte[]{1, 2, 3};

      when(objectMapper.writeValueAsBytes(any(ErrorResponse.class)))
          .thenReturn(expectedBytes);

      // When
      byte[] result = errorResponseBuilder.buildErrorResponse(errorType, message);

      // Then
      assertThat(result).isEqualTo(expectedBytes);
      verify(objectMapper, times(1)).writeValueAsBytes(any(ErrorResponse.class));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void buildErrorResponse_SerializationException() {
    try {
      // Given
      ErrorType errorType = ErrorType.KEYCLOAK_AUTH_FAILED;
      String message = "Token validation failed";
      com.fasterxml.jackson.core.JsonProcessingException exception =
          new com.fasterxml.jackson.core.JsonProcessingException("Serialization error") {};

      when(objectMapper.writeValueAsBytes(any(ErrorResponse.class)))
          .thenThrow(exception);

      // When & Then
      assertThatThrownBy(() -> errorResponseBuilder.buildErrorResponse(errorType, message))
          .isInstanceOf(ResponseSerializationException.class)
          .hasMessageContaining("ErrorType response serialization failed");

      verify(objectMapper, times(1)).writeValueAsBytes(any(ErrorResponse.class));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void buildApiErrorResponse_Success() {
    try {
      // Given
      ResponseCode responseCode = ResponseCode.UNAUTHORIZED;
      String message = "User not authenticated";
      byte[] expectedBytes = new byte[]{4, 5, 6};

      when(objectMapper.writeValueAsBytes(any(ApiResponse.class)))
          .thenReturn(expectedBytes);

      // When
      byte[] result = errorResponseBuilder.buildApiErrorResponse(responseCode, message);

      // Then
      assertThat(result).isEqualTo(expectedBytes);
      verify(objectMapper, times(1)).writeValueAsBytes(any(ApiResponse.class));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void buildApiErrorResponse_SerializationException() {
    try {
      // Given
      ResponseCode responseCode = ResponseCode.UNAUTHORIZED;
      String message = "User not authenticated";
      com.fasterxml.jackson.core.JsonProcessingException exception =
          new com.fasterxml.jackson.core.JsonProcessingException("Serialization error") {};

      when(objectMapper.writeValueAsBytes(any(ApiResponse.class)))
          .thenThrow(exception);

      // When & Then
      assertThatThrownBy(() -> errorResponseBuilder.buildApiErrorResponse(responseCode, message))
          .isInstanceOf(ResponseSerializationException.class)
          .hasMessageContaining("ApiResponse serialization failed");

      verify(objectMapper, times(1)).writeValueAsBytes(any(ApiResponse.class));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}

