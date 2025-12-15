package com.oregonMarkets.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.exception.ResponseSerializationException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.dto.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ErrorResponseBuilderTest {

    private ErrorResponseBuilder builder;
    
    @Mock
    private ObjectMapper mockObjectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        builder = new ErrorResponseBuilder(new ObjectMapper());
    }

    @Test
    void shouldBuildErrorResponse() {
        byte[] result = builder.buildErrorResponse(ErrorType.MAGIC_AUTH_FAILED, "Test message");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void shouldBuildApiErrorResponse() {
        byte[] result = builder.buildApiErrorResponse(ResponseCode.UNAUTHORIZED, "Test message");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
        ErrorResponseBuilder failingBuilder = new ErrorResponseBuilder(mockObjectMapper);
        when(mockObjectMapper.writeValueAsBytes(any())).thenThrow(new JsonProcessingException("Test error") {});

        assertThrows(ResponseSerializationException.class, () -> 
            failingBuilder.buildErrorResponse(ErrorType.MAGIC_AUTH_FAILED, "Test"));
    }

    @Test
    void shouldThrowExceptionWhenApiSerializationFails() throws JsonProcessingException {
        ErrorResponseBuilder failingBuilder = new ErrorResponseBuilder(mockObjectMapper);
        when(mockObjectMapper.writeValueAsBytes(any())).thenThrow(new JsonProcessingException("Test error") {});

        assertThrows(ResponseSerializationException.class, () -> 
            failingBuilder.buildApiErrorResponse(ResponseCode.UNAUTHORIZED, "Test"));
    }
}
