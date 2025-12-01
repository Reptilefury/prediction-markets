package com.oregonMarkets.common.exception;

import com.oregonMarkets.common.response.ApiResponse;
import com.oregonMarkets.common.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for Oregon Markets
 * Maps exceptions to standardized response codes and formats
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        String traceId = generateTraceId();
        log.warn("Business exception [{}]: {} - {}", traceId, ex.getResponseCode(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getResponseCode(), ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        String traceId = generateTraceId();
        log.warn("User already exists [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.DUPLICATE_USER, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(MagicAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleMagicAuth(MagicAuthException ex) {
        String traceId = generateTraceId();
        log.error("Magic authentication error [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.MAGIC_AUTH_FAILED, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(EnclaveApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleEnclaveApi(EnclaveApiException ex) {
        String traceId = generateTraceId();
        log.error("Enclave API error [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.ENCLAVE_API_ERROR, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(BlnkApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlnkApi(BlnkApiException ex) {
        String traceId = generateTraceId();
        log.error("Blnk API error [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.BLNK_API_ERROR, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(Web3AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleWeb3Auth(Web3AuthException ex) {
        String traceId = generateTraceId();
        log.error("Web3 authentication error [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.WEB3_AUTH_FAILED, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(KeycloakAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeycloakAuth(KeycloakAuthException ex) {
        String traceId = generateTraceId();
        log.error("Keycloak authentication error [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.KEYCLOAK_AUTH_FAILED, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(BlockchainException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlockchain(BlockchainException ex) {
        String traceId = generateTraceId();
        log.error("Blockchain error [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.BLOCKCHAIN_ERROR, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(ExternalServiceException ex) {
        String traceId = generateTraceId();
        log.error("External service error [{}]: {} - Service: {}", traceId, ex.getMessage(), ex.getServiceName());

        ApiResponse<Void> response = ApiResponse.error(ex.getResponseCode(), ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String traceId = generateTraceId();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation exception [{}]: {}", traceId, errors);

        ApiResponse<Void> response = ApiResponse.validationError(errors);
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebExchangeValidation(WebExchangeBindException ex) {
        String traceId = generateTraceId();
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");

        log.warn("WebExchange validation exception [{}]: {}", traceId, message);

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.VALIDATION_ERROR, message);
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        String traceId = generateTraceId();
        log.warn("Authentication failed [{}]: {}", traceId, ex.getMessage());

        ResponseCode responseCode = ex instanceof BadCredentialsException
                ? ResponseCode.INVALID_CREDENTIALS
                : ResponseCode.UNAUTHORIZED;

        ApiResponse<Void> response = ApiResponse.error(responseCode);
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        String traceId = generateTraceId();
        log.warn("Access denied [{}]: {}", traceId, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.FORBIDDEN, ex.getMessage());
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        String traceId = generateTraceId();
        log.warn("Duplicate key violation [{}]: {}", traceId, ex.getMessage());

        String errorMessage = "User with this email already exists";
        if (ex.getMessage() != null && ex.getMessage().contains("users_magic_user_id_key")) {
            errorMessage = "User with this Magic ID already exists";
        }

        ApiResponse<Void> response = ApiResponse.error(
                ResponseCode.DUPLICATE_USER,
                errorMessage
        );
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
        String traceId = generateTraceId();
        log.error("Database error [{}]: {}", traceId, ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                ResponseCode.DATABASE_ERROR,
                "A database error occurred. Please try again."
        );
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        String traceId = generateTraceId();
        log.error("Unexpected error [{}]: {}", traceId, ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                ResponseCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again or contact support."
        );
        response.getError().setTraceId(traceId);

        return response.toResponseEntity();
    }

    /**
     * Generate a unique trace ID for error tracking
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}