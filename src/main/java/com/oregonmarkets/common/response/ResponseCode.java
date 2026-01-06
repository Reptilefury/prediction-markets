package com.oregonmarkets.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Comprehensive response code system for Oregon Markets Code ranges: 2000-2999: Success codes
 * 3000-3999: Client errors (4xx) 4000-4999: Server errors (5xx) 5000+: Critical system errors
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

  // Deposit Operations (2100-2199)
  DEPOSIT_INITIATED(2100, "Deposit initiated successfully", HttpStatus.OK),
  DEPOSIT_PENDING(2101, "Deposit is pending blockchain confirmation", HttpStatus.OK),
  DEPOSIT_CONFIRMED(2102, "Deposit confirmed and credited", HttpStatus.OK),
  DEPOSIT_DETECTED(2103, "Deposit detected on blockchain", HttpStatus.OK),

  // Withdrawal Operations (2200-2299)
  WITHDRAWAL_INITIATED(2200, "Withdrawal initiated successfully", HttpStatus.OK),
  WITHDRAWAL_PENDING(2201, "Withdrawal pending approval", HttpStatus.OK),
  WITHDRAWAL_APPROVED(2202, "Withdrawal approved", HttpStatus.OK),
  WITHDRAWAL_COMPLETED(2203, "Withdrawal completed successfully", HttpStatus.OK),
  WITHDRAWAL_QUEUED(2204, "Withdrawal queued for processing", HttpStatus.OK),

  // Market Operations (2300-2399)
  MARKET_CREATED(2300, "Market created successfully", HttpStatus.CREATED),
  MARKET_UPDATED(2301, "Market updated successfully", HttpStatus.OK),
  MARKET_RESOLVED(2302, "Market resolved successfully", HttpStatus.OK),

  // Authentication/User Operations (2400-2449)
  USER_REGISTERED(2400, "User registered successfully", HttpStatus.CREATED),
  USER_AUTHENTICATED(2401, "User authenticated successfully", HttpStatus.OK),
  USER_PROFILE_UPDATED(2402, "User profile updated successfully", HttpStatus.OK),
  USER_PROFILE_RETRIEVED(2403, "User profile retrieved successfully", HttpStatus.OK),
  TOKEN_REFRESHED(2404, "Authentication token refreshed", HttpStatus.OK),
  PASSWORD_RESET(2405, "Password reset successfully", HttpStatus.OK),

  // Admin Operations (2450-2499)
  ADMIN_USER_CREATED(2450, "Admin user created successfully", HttpStatus.CREATED),
  ADMIN_USER_UPDATED(2451, "Admin user updated successfully", HttpStatus.OK),
  ADMIN_USER_DELETED(2452, "Admin user deleted successfully", HttpStatus.OK),
  ADMIN_USER_RETRIEVED(2453, "Admin user retrieved successfully", HttpStatus.OK),
  ADMIN_USERS_LISTED(2454, "Admin users listed successfully", HttpStatus.OK),
  ADMIN_LOGIN_UPDATED(2455, "Admin last login updated successfully", HttpStatus.OK),
  ADMIN_ROLE_ASSIGNED(2456, "Admin role assigned successfully", HttpStatus.OK),
  ADMIN_PERMISSIONS_UPDATED(2457, "Admin permissions updated successfully", HttpStatus.OK),

  // ============================================
  // CLIENT ERRORS (3000-3999) - HTTP 4xx
  // ============================================

  // Trading Operations (2500-2599)
  ORDER_PLACED(2500, "Order placed successfully", HttpStatus.CREATED),
  ORDER_MATCHED(2501, "Order matched successfully", HttpStatus.OK),
  ORDER_CANCELLED(2502, "Order cancelled successfully", HttpStatus.OK),
  ORDER_PARTIALLY_FILLED(2503, "Order partially filled", HttpStatus.OK),
  ORDER_FILLED(2504, "Order completely filled", HttpStatus.OK),

  // ============================================
  // CLIENT ERRORS (3000-3999) - HTTP 4xx
  // ============================================

  // Validation Errors (3000-3099)
  VALIDATION_ERROR(3000, "Validation failed", HttpStatus.BAD_REQUEST),
  INVALID_INPUT(3001, "Invalid input provided", HttpStatus.BAD_REQUEST),
  MISSING_REQUIRED_FIELD(3002, "Required field is missing", HttpStatus.BAD_REQUEST),
  INVALID_FORMAT(3003, "Invalid format", HttpStatus.BAD_REQUEST),
  INVALID_AMOUNT(3004, "Invalid amount specified", HttpStatus.BAD_REQUEST),
  AMOUNT_TOO_LOW(3005, "Amount is below minimum required", HttpStatus.BAD_REQUEST),
  AMOUNT_TOO_HIGH(3006, "Amount exceeds maximum allowed", HttpStatus.BAD_REQUEST),
  INVALID_ADDRESS(3007, "Invalid wallet address", HttpStatus.BAD_REQUEST),
  INVALID_DATE_RANGE(3008, "Invalid date range specified", HttpStatus.BAD_REQUEST),

  // Authentication/Authorization Errors (3100-3199)
  UNAUTHORIZED(3100, "Authentication required", HttpStatus.UNAUTHORIZED),
  INVALID_CREDENTIALS(3101, "Invalid credentials provided", HttpStatus.UNAUTHORIZED),
  TOKEN_EXPIRED(3102, "Authentication token has expired", HttpStatus.UNAUTHORIZED),
  TOKEN_INVALID(3103, "Invalid authentication token", HttpStatus.UNAUTHORIZED),
  FORBIDDEN(3104, "Access forbidden", HttpStatus.FORBIDDEN),
  INSUFFICIENT_PERMISSIONS(
      3105, "Insufficient permissions for this operation", HttpStatus.FORBIDDEN),
  SESSION_EXPIRED(3106, "Session has expired", HttpStatus.UNAUTHORIZED),
  ACCOUNT_LOCKED(3107, "Account is locked", HttpStatus.FORBIDDEN),
  ACCOUNT_SUSPENDED(3108, "Account is suspended", HttpStatus.FORBIDDEN),
  EMAIL_NOT_VERIFIED(3109, "Email not verified", HttpStatus.FORBIDDEN),
  KYC_REQUIRED(3118, "KYC verification required", HttpStatus.FORBIDDEN),
  KYC_PENDING(3119, "KYC verification pending", HttpStatus.FORBIDDEN),

  // Resource Not Found (3200-3299)
  NOT_FOUND(3200, "Resource not found", HttpStatus.NOT_FOUND),
  USER_NOT_FOUND(3201, "User not found", HttpStatus.NOT_FOUND),
  MARKET_NOT_FOUND(3202, "Market not found", HttpStatus.NOT_FOUND),
  ORDER_NOT_FOUND(3203, "Order not found", HttpStatus.NOT_FOUND),
  TRANSACTION_NOT_FOUND(3204, "Transaction not found", HttpStatus.NOT_FOUND),
  WALLET_NOT_FOUND(3205, "Wallet not found", HttpStatus.NOT_FOUND),
  DEPOSIT_NOT_FOUND(3206, "Deposit not found", HttpStatus.NOT_FOUND),
  WITHDRAWAL_NOT_FOUND(3207, "Withdrawal not found", HttpStatus.NOT_FOUND),

  // Admin Resource Not Found (3250-3299)
  ADMIN_USER_NOT_FOUND(3250, "Admin user not found", HttpStatus.NOT_FOUND),
  ADMIN_ROLE_NOT_FOUND(3251, "Admin role not found", HttpStatus.NOT_FOUND),
  ADMIN_PERMISSION_NOT_FOUND(3252, "Admin permission not found", HttpStatus.NOT_FOUND),
  ADMIN_USER_EMAIL_EXISTS(3253, "Admin user with this email already exists", HttpStatus.CONFLICT),
  ADMIN_ROLE_IN_USE(3254, "Admin role is currently in use and cannot be deleted", HttpStatus.CONFLICT),
  ADMIN_INVALID_ROLE_ASSIGNMENT(3255, "Invalid role assignment for admin user", HttpStatus.BAD_REQUEST),

  // Business Logic Errors (3300-3399)
  INSUFFICIENT_BALANCE(3300, "Insufficient balance", HttpStatus.BAD_REQUEST),
  INSUFFICIENT_LIQUIDITY(3301, "Insufficient market liquidity", HttpStatus.BAD_REQUEST),
  MARKET_CLOSED(3302, "Market is closed for trading", HttpStatus.BAD_REQUEST),
  MARKET_ALREADY_RESOLVED(3303, "Market has already been resolved", HttpStatus.BAD_REQUEST),
  DUPLICATE_ORDER(3304, "Duplicate order detected", HttpStatus.CONFLICT),
  ORDER_ALREADY_CANCELLED(3305, "Order is already cancelled", HttpStatus.BAD_REQUEST),
  ORDER_ALREADY_FILLED(3306, "Order is already filled", HttpStatus.BAD_REQUEST),
  CANNOT_CANCEL_ORDER(3307, "Cannot cancel order in current state", HttpStatus.BAD_REQUEST),
  SELF_TRADE_NOT_ALLOWED(3308, "Self-trading is not allowed", HttpStatus.BAD_REQUEST),
  POSITION_LIMIT_EXCEEDED(3309, "Position limit exceeded", HttpStatus.BAD_REQUEST),
  MINIMUM_ORDER_SIZE(3310, "Order size below minimum", HttpStatus.BAD_REQUEST),
  MAXIMUM_ORDER_SIZE(3311, "Order size exceeds maximum", HttpStatus.BAD_REQUEST),
  INVALID_PRICE(3312, "Invalid price specified", HttpStatus.BAD_REQUEST),
  PRICE_OUT_OF_RANGE(3313, "Price is out of valid range", HttpStatus.BAD_REQUEST),
  WITHDRAWAL_LIMIT_EXCEEDED(3314, "Withdrawal limit exceeded", HttpStatus.BAD_REQUEST),
  DEPOSIT_LIMIT_EXCEEDED(3315, "Deposit limit exceeded", HttpStatus.BAD_REQUEST),
  DAILY_LIMIT_EXCEEDED(3316, "Daily transaction limit exceeded", HttpStatus.BAD_REQUEST),
  PENDING_WITHDRAWAL_EXISTS(3317, "Pending withdrawal already exists", HttpStatus.CONFLICT),
  USER_ALREADY_EXISTS(3320, "User already exists", HttpStatus.CONFLICT),
  DUPLICATE_USER(3320, "User already exists", HttpStatus.CONFLICT),
  ROLE_REQUIRES_PERMISSIONS(3321, "Role must have at least one permission", HttpStatus.BAD_REQUEST),
  DUPLICATE_ROLE(3322, "Role already exists", HttpStatus.CONFLICT),
  DUPLICATE_PERMISSION(3323, "Permission already exists", HttpStatus.CONFLICT),

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
  CONSTRAINT_VIOLATION(4105, "Database constraint violation", HttpStatus.INTERNAL_SERVER_ERROR),

  // External Service Errors (4200-4299)
  EXTERNAL_SERVICE_ERROR(4200, "External service error", HttpStatus.BAD_GATEWAY),
  BLOCKCHAIN_ERROR(4201, "Blockchain service error", HttpStatus.BAD_GATEWAY),
  BLOCKCHAIN_TIMEOUT(4202, "Blockchain operation timeout", HttpStatus.GATEWAY_TIMEOUT),
  BLOCKCHAIN_NODE_UNAVAILABLE(4203, "Blockchain node unavailable", HttpStatus.SERVICE_UNAVAILABLE),
  PAYMENT_PROVIDER_ERROR(4204, "Payment provider error", HttpStatus.BAD_GATEWAY),
  PAYMENT_PROVIDER_TIMEOUT(4205, "Payment provider timeout", HttpStatus.GATEWAY_TIMEOUT),
  WALLET_SERVICE_ERROR(4206, "Wallet service error", HttpStatus.BAD_GATEWAY),
  ORACLE_SERVICE_ERROR(4207, "Oracle service error", HttpStatus.BAD_GATEWAY),
  NOTIFICATION_SERVICE_ERROR(4208, "Notification service error", HttpStatus.INTERNAL_SERVER_ERROR),
  MAGIC_AUTH_FAILED(4210, "Magic authentication failed", HttpStatus.UNAUTHORIZED),
  ENCLAVE_API_ERROR(4211, "Enclave API error", HttpStatus.BAD_GATEWAY),
  BLNK_API_ERROR(4212, "Blnk API error", HttpStatus.BAD_GATEWAY),
  WEB3_AUTH_FAILED(4213, "Web3 authentication failed", HttpStatus.UNAUTHORIZED),
  KEYCLOAK_AUTH_FAILED(4214, "Keycloak authentication failed", HttpStatus.UNAUTHORIZED),

  // Configuration Errors (4300-4399)
  CONFIGURATION_ERROR(4300, "Configuration error", HttpStatus.INTERNAL_SERVER_ERROR),
  MISSING_CONFIGURATION(4301, "Missing required configuration", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_CONFIGURATION(4302, "Invalid configuration", HttpStatus.INTERNAL_SERVER_ERROR),

  // Integration Errors (4400-4499)
  INTEGRATION_ERROR(4400, "Integration error", HttpStatus.INTERNAL_SERVER_ERROR),
  BLNK_INTEGRATION_ERROR(4401, "Blnk Core integration error", HttpStatus.BAD_GATEWAY),
  ENCLAVE_INTEGRATION_ERROR(4402, "Enclave UDA integration error", HttpStatus.BAD_GATEWAY),
  DYNAMIC_AUTH_ERROR(4403, "Dynamic.xyz authentication error", HttpStatus.BAD_GATEWAY),

  // ============================================
  // CRITICAL SYSTEM ERRORS (5000+) - HTTP 5xx
  // ============================================

  // Critical Payment Errors (5000-5099)
  CRITICAL_PAYMENT_ERROR(
      5000, "Critical payment processing error", HttpStatus.INTERNAL_SERVER_ERROR),
  DOUBLE_SPEND_DETECTED(5001, "Double spend attempt detected", HttpStatus.INTERNAL_SERVER_ERROR),
  BALANCE_MISMATCH(5002, "Account balance mismatch detected", HttpStatus.INTERNAL_SERVER_ERROR),
  RECONCILIATION_ERROR(5003, "Reconciliation error detected", HttpStatus.INTERNAL_SERVER_ERROR),
  FUNDS_LOCKED(5004, "Funds are locked due to system error", HttpStatus.INTERNAL_SERVER_ERROR),

  // Critical Security Events (5100-5199)
  SECURITY_BREACH_DETECTED(5100, "Security breach detected", HttpStatus.INTERNAL_SERVER_ERROR),
  SUSPICIOUS_ACTIVITY(5101, "Suspicious activity detected", HttpStatus.FORBIDDEN),
  FRAUD_DETECTED(5102, "Fraudulent activity detected", HttpStatus.FORBIDDEN),
  MULTIPLE_FAILED_ATTEMPTS(5103, "Multiple failed attempts detected", HttpStatus.FORBIDDEN);

  private final int code;
  private final String message;
  private final HttpStatus httpStatus;

  ResponseCode(int code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }

  /** Get ResponseCode by code number */
  public static ResponseCode fromCode(int code) {
    for (ResponseCode rc : values()) {
      if (rc.code == code) {
        return rc;
      }
    }
    return UNEXPECTED_ERROR;
  }

  /** Check if this is a success code */
  public boolean isSuccess() {
    return code >= 2000 && code < 3000;
  }

  /** Check if this is a client error */
  public boolean isClientError() {
    return code >= 3000 && code < 4000;
  }

  /** Check if this is a server error */
  public boolean isServerError() {
    return code >= 4000;
  }

  /** Check if this is a critical error */
  public boolean isCritical() {
    return code >= 5000;
  }
}
