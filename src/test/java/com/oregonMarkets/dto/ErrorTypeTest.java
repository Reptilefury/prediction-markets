package com.oregonMarkets.dto;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ErrorTypeTest {

    @Test
    void getCode() {
        assertEquals("USER_ALREADY_EXISTS", ErrorType.USER_ALREADY_EXISTS.getCode());
        assertEquals("MAGIC_AUTH_FAILED", ErrorType.MAGIC_AUTH_FAILED.getCode());
    }

    @Test
    void getHttpStatus() {
        assertEquals(HttpStatus.CONFLICT, ErrorType.USER_ALREADY_EXISTS.getHttpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorType.MAGIC_AUTH_FAILED.getHttpStatus());
    }

    @Test
    void getStatusCode() {
        assertEquals(3320, ErrorType.USER_ALREADY_EXISTS.getStatusCode());
        assertEquals(4210, ErrorType.MAGIC_AUTH_FAILED.getStatusCode());
    }

    @Test
    void getDefaultMessage() {
        assertEquals("User already exists", ErrorType.USER_ALREADY_EXISTS.getDefaultMessage());
        assertEquals("Magic authentication failed", ErrorType.MAGIC_AUTH_FAILED.getDefaultMessage());
    }
}