package com.oregonMarkets.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleExceptionsTest {

    @Test
    void userAlreadyExistsException_WithMessage() {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("User exists");
        assertEquals("User with email 'User exists' already exists", exception.getMessage());
    }

    @Test
    void magicAuthException_WithMessage() {
        MagicAuthException exception = new MagicAuthException("Magic auth failed");
        assertEquals("Magic auth failed", exception.getMessage());
    }

    @Test
    void magicAuthException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Token invalid");
        MagicAuthException exception = new MagicAuthException("Magic auth failed", cause);
        assertEquals("Magic auth failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void enclaveApiException_WithMessage() {
        EnclaveApiException exception = new EnclaveApiException("Enclave error");
        assertEquals("Enclave error", exception.getMessage());
    }

    @Test
    void enclaveApiException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("API error");
        EnclaveApiException exception = new EnclaveApiException("Enclave error", cause);
        assertEquals("Enclave error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void blnkApiException_WithMessage() {
        BlnkApiException exception = new BlnkApiException("Blnk error");
        assertEquals("Blnk error", exception.getMessage());
    }

    @Test
    void blnkApiException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("HTTP error");
        BlnkApiException exception = new BlnkApiException("Blnk error", cause);
        assertEquals("Blnk error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void web3AuthException_WithMessage() {
        Web3AuthException exception = new Web3AuthException("Web3 error");
        assertEquals("Web3 error", exception.getMessage());
    }

    @Test
    void web3AuthException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Signature invalid");
        Web3AuthException exception = new Web3AuthException("Web3 error", cause);
        assertEquals("Web3 error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void keycloakAuthException_WithMessage() {
        KeycloakAuthException exception = new KeycloakAuthException("Keycloak error");
        assertEquals("Keycloak error", exception.getMessage());
    }

    @Test
    void keycloakAuthException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Token expired");
        KeycloakAuthException exception = new KeycloakAuthException("Keycloak error", cause);
        assertEquals("Keycloak error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void blockchainException_WithMessage() {
        BlockchainException exception = new BlockchainException("Blockchain error");
        assertEquals("Blockchain error", exception.getMessage());
    }

    @Test
    void blockchainException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("RPC error");
        BlockchainException exception = new BlockchainException("Blockchain error", cause);
        assertEquals("Blockchain error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void responseSerializationException_WithMessage() {
        ResponseSerializationException exception = new ResponseSerializationException("Serialization error");
        assertEquals("Failed to serialize response: Serialization error", exception.getMessage());
    }

    @Test
    void responseSerializationException_WithMessageAndCause() {
        RuntimeException cause = new RuntimeException("JSON error");
        ResponseSerializationException exception = new ResponseSerializationException("Serialization error", cause);
        assertEquals("Failed to serialize response: Serialization error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}