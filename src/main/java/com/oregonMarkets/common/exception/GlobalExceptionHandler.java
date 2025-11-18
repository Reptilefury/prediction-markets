package com.oregonMarkets.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(createErrorResponse("USER_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(MagicAuthException.class)
    public ResponseEntity<Map<String, Object>> handleMagicAuth(MagicAuthException ex) {
        log.error("Magic authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse("MAGIC_AUTH_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(EnclaveApiException.class)
    public ResponseEntity<Map<String, Object>> handleEnclaveApi(EnclaveApiException ex) {
        log.error("Enclave API error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(createErrorResponse("ENCLAVE_API_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(BlnkApiException.class)
    public ResponseEntity<Map<String, Object>> handleBlnkApi(BlnkApiException ex) {
        log.error("Blnk API error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(createErrorResponse("BLNK_API_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(Web3AuthException.class)
    public ResponseEntity<Map<String, Object>> handleWeb3Auth(Web3AuthException ex) {
        log.error("Web3 authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse("WEB3_AUTH_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(BlockchainException.class)
    public ResponseEntity<Map<String, Object>> handleBlockchain(BlockchainException ex) {
        log.error("Blockchain error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createErrorResponse("BLOCKCHAIN_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        log.error("Business error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("BUSINESS_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> createErrorResponse(String code, String message) {
        return Map.of(
            "error", code,
            "message", message,
            "timestamp", Instant.now()
        );
    }
}