package com.oregonMarkets.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void from_WithMessage() {
        ErrorResponse response = ErrorResponse.from(ErrorType.USER_ALREADY_EXISTS, "Custom message");
        
        assertEquals("USER_ALREADY_EXISTS", response.getErrorCode());
        assertEquals("Custom message", response.getErrorMessage());
        assertEquals("3320", response.getStatusCode());
        assertEquals("FAILED", response.getStatus());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void from_WithNullMessage() {
        ErrorResponse response = ErrorResponse.from(ErrorType.MAGIC_AUTH_FAILED, null);
        
        assertEquals("MAGIC_AUTH_FAILED", response.getErrorCode());
        assertEquals("Magic authentication failed", response.getErrorMessage());
        assertEquals("4210", response.getStatusCode());
    }
}