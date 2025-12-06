package com.oregonMarkets.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.dto.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ErrorResponseBuilderTest {

  @Mock private ObjectMapper objectMapper;

  private ErrorResponseBuilder errorResponseBuilder;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    errorResponseBuilder = new ErrorResponseBuilder(objectMapper);
  }

  @Test
  void buildErrorResponse_ShouldReturnBytes() throws Exception {
    byte[] expected = "test".getBytes();
    when(objectMapper.writeValueAsBytes(any())).thenReturn(expected);

    byte[] result =
        errorResponseBuilder.buildErrorResponse(ErrorType.MAGIC_AUTH_FAILED, "Test message");

    assertArrayEquals(expected, result);
  }

  @Test
  void buildApiErrorResponse_ShouldReturnBytes() throws Exception {
    byte[] expected = "test".getBytes();
    when(objectMapper.writeValueAsBytes(any())).thenReturn(expected);

    byte[] result =
        errorResponseBuilder.buildApiErrorResponse(ResponseCode.UNAUTHORIZED, "Test message");

    assertArrayEquals(expected, result);
  }
}
