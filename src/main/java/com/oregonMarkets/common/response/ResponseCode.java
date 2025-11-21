package com.oregonMarkets.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Comprehensive response code system for Oregon Markets
 * Code ranges:
 * 2000-2999: Success codes
 * 3000-3999: Client errors (4xx)
 * 4000-4999: Server errors (5xx)
 * 5000+: Critical system errors
 */
@Getter
public enum ResponseCode {

    // ============================================
    // SUCCESS CODES (2000-2999) - HTTP 200/201
    // ============================================

    // General Success (2000-2099)
    SUCCESS(2000, "Operation completed successfully", HttpStatus.OK),
    CREATED(2001, "Resource created successfully", HttpStatus.CREATED),
    UPDATED(2002, "Resource updated successfully", HttpStatus.OK),
    DELETED(2003, "Resource deleted successfully", HttpStatus.OK),

    // Authentication/User Operations (2400-2499)
    USER_REGISTERED(2400, "User registered successfully", HttpStatus.CREATED),
    USER_AUTHENTICATED(2401, "User authenticated successfully", HttpStatus.OK),
    USER_PROFILE_UPDATED(2402, "User profile updated successfully", HttpStatus.OK),
    TOKEN_REFRESHED(2403, "Authentication token refreshed", HttpStatus.OK),
    PASSWORD_RESET(2404, "Password reset successfully", HttpStatus.OK),

    // ============================================
    // CLIENT ERRORS (3000-3999) - HTTP 4xx
    // ============================================

    // Validation Errors (3000-3099)
    VALIDATION_ERROR(3000, "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(3001, "Invalid input provided", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(3002, "Required field is missing", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT(3003, "Invalid format", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(3007, "Invalid wallet address", HttpStatus.BAD_REQUEST),

    // Authentication/Authorization Errors (3100-3199)
    UNAUTHORIZED(3100, "Authentication required", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(3101, "Invalid credentials provided", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(3102, "Authentication token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(3103, "Invalid authentication token", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(3104, "Access forbidden", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS(3105, "Insufficient permissions for this operation", HttpStatus.FORBIDDEN),
    SESSION_EXPIRED(3106, "Session has expired", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(3107, "Account is locked", HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED(3108, "Account is suspended", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(3109, "Email not verified", HttpStatus.FORBIDDEN),
    KYC_REQUIRED(3118, "KYC verification required", HttpStatus.FORBIDDEN),
    KYC_PENDING(3119, "KYC verification pending", HttpStatus.FORBIDDEN),

    // Resource Not Found (3200-3299)
    NOT_FOUND(3200, "Resource not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(3201, "User not found", HttpStatus.NOT_FOUND),

    // Business Logic Errors (3300-3399)
    INSUFFICIENT_BALANCE(3300, "Insufficient balance", HttpStatus.BAD_REQUEST),
    DUPLICATE_USER(3304, "User already exists", HttpStatus.CONFLICT),

    // Rate Limiting/Quota Errors (3400-3499)
    RATE_LIMIT_EXCEEDED(3400, "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    TOO_MANY_REQUESTS(3401, "Too many requests", HttpStatus.TOO_MANY_REQUESTS),

    // ============================================
    // SERVER ERRORS (4000-4999) - HTTP 5xx
    // ============================================

    // General Server Errors (4000-4099)
    INTERNAL_SERVER_ERROR(4000, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(4001, "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT_ERROR(4002, "Request timeout", HttpStatus.GATEWAY_TIMEOUT),
    PROCESSING_ERROR(4003, "Error processing request", HttpStatus.INTERNAL_SERVER_ERROR),
    UNEXPECTED_ERROR(4004, "Unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),

    // Database Errors (4100-4199)
    DATABASE_ERROR(4100, "Database error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_CONNECTION_FAILED(4101, "Database connection failed", HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_TIMEOUT(4102, "Database operation timeout", HttpStatus.GATEWAY_TIMEOUT),
    TRANSACTION_FAILED(4103, "Database transaction failed", HttpStatus.INTERNAL_SERVER_ERROR),
    DEADLOCK_DETECTED(4104, "Database deadlock detected", HttpStatus.INTERNAL_SERVER_ERROR),

    // External Service Errors (4200-4299)
    EXTERNAL_SERVICE_ERROR(4200, "External service error", HttpStatus.BAD_GATEWAY),
    BLOCKCHAIN_ERROR(4201, "Blockchain service error", HttpStatus.BAD_GATEWAY),
    BLOCKCHAIN_TIMEOUT(4202, "Blockchain operation timeout", HttpStatus.GATEWAY_TIMEOUT),
    MAGIC_AUTH_FAILED(4210, "Magic authentication failed", HttpStatus.UNAUTHORIZED),
    ENCLAVE_API_ERROR(4211, "Enclave API error", HttpStatus.BAD_GATEWAY),
    BLNK_API_ERROR(4212, "Blnk API error", HttpStatus.BAD_GATEWAY),
    WEB3_AUTH_FAILED(4213, "Web3 authentication failed", HttpStatus.UNAUTHORIZED),
    KEYCLOAK_AUTH_FAILED(4214, "Keycloak authentication failed", HttpStatus.UNAUTHORIZED),

    // ============================================
    // CRITICAL SYSTEM ERRORS (5000+) - HTTP 5xx
    // ============================================

    // Critical Payment Errors (5000-5099)
    CRITICAL_PAYMENT_ERROR(5000, "Critical payment processing error", HttpStatus.INTERNAL_SERVER_ERROR),
    DOUBLE_SPEND_DETECTED(5001, "Double spend attempt detected", HttpStatus.INTERNAL_SERVER_ERROR),
    BALANCE_MISMATCH(5002, "Account balance mismatch detected", HttpStatus.INTERNAL_SERVER_ERROR),

    // Critical Security Events (5100-5199)
    SECURITY_BREACH_DETECTED(5100, "Security breach detected", HttpStatus.INTERNAL_SERVER_ERROR),
    SUSPICIOUS_ACTIVITY(5101, "Suspicious activity detected", HttpStatus.FORBIDDEN),
    FRAUD_DETECTED(5102, "Fraudulent activity detected", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ResponseCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Get ResponseCode by code number
     */
    public static ResponseCode fromCode(int code) {
        for (ResponseCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        return UNEXPECTED_ERROR;
    }

    /**
     * Check if this is a success code
     */
    public boolean isSuccess() {
        return code >= 2000 && code < 3000;
    }

    /**
     * Check if this is a client error
     */
    public boolean isClientError() {
        return code >= 3000 && code < 4000;
    }

    /**
     * Check if this is a server error
     */
    public boolean isServerError() {
        return code >= 4000;
    }

    /**
     * Check if this is a critical error
     */
    public boolean isCritical() {
        return code >= 5000;
    }
}
