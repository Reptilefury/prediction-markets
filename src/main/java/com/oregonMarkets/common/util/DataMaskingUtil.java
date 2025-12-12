package com.oregonMarkets.common.util;

/**
 * Utility class for masking sensitive data in logs
 * Ensures PII and sensitive information is not exposed in application logs
 */
public class DataMaskingUtil {

  private static final String MASK_CHAR = "*";
  private static final int VISIBLE_PREFIX_LENGTH = 4;
  private static final int VISIBLE_SUFFIX_LENGTH = 4;

  private DataMaskingUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Mask email address - shows first 2 chars and domain
   * Example: user@example.com -> us***@example.com
   */
  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return "[empty]";
    }

    int atIndex = email.indexOf('@');
    if (atIndex <= 0) {
      return maskString(email, 2, 0);
    }

    String localPart = email.substring(0, atIndex);
    String domain = email.substring(atIndex);

    String maskedLocal = maskString(localPart, 2, 0);
    return maskedLocal + domain;
  }

  /**
   * Mask wallet address - shows first 6 and last 4 characters
   * Example: 0x1234567890abcdef -> 0x1234...cdef
   */
  public static String maskWalletAddress(String address) {
    if (address == null || address.isEmpty()) {
      return "[empty]";
    }

    if (address.length() <= 10) {
      return maskString(address, 2, 2);
    }

    return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
  }

  /**
   * Mask user ID - shows first 8 characters only
   * Example: 550e8400-e29b-41d4-a716-446655440000 -> 550e8400-****
   */
  public static String maskUserId(String userId) {
    if (userId == null || userId.isEmpty()) {
      return "[empty]";
    }

    if (userId.length() <= 8) {
      return maskString(userId, 4, 0);
    }

    return userId.substring(0, 8) + "-****";
  }

  /**
   * Generic string masking utility
   * Shows specified number of characters at start and end
   */
  public static String maskString(String value, int prefixLength, int suffixLength) {
    if (value == null || value.isEmpty()) {
      return "[empty]";
    }

    int length = value.length();
    if (length <= prefixLength + suffixLength) {
      return MASK_CHAR.repeat(Math.max(1, length));
    }

    String prefix = value.substring(0, prefixLength);
    String suffix = suffixLength > 0 ? value.substring(length - suffixLength) : "";
    int maskedLength = length - prefixLength - suffixLength;

    return prefix + MASK_CHAR.repeat(maskedLength) + suffix;
  }

  /**
   * Sanitize error response body - truncate and remove potential sensitive data
   * Shows first 100 chars only to prevent log bloat and data leakage
   */
  public static String sanitizeErrorBody(String errorBody) {
    if (errorBody == null || errorBody.isEmpty()) {
      return "[empty response]";
    }

    // Remove common sensitive field patterns
    String sanitized = errorBody
        .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
        .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
        .replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"")
        .replaceAll("\"apiKey\"\\s*:\\s*\"[^\"]*\"", "\"apiKey\":\"***\"")
        .replaceAll("\"authorization\"\\s*:\\s*\"[^\"]*\"", "\"authorization\":\"***\"");

    // Truncate to first 200 chars to prevent log bloat
    if (sanitized.length() > 200) {
      return sanitized.substring(0, 200) + "... [truncated]";
    }

    return sanitized;
  }

  /**
   * Mask response object - for debug logging
   * Returns a safe representation without sensitive data
   */
  public static String maskResponseObject(Object response) {
    if (response == null) {
      return "[null]";
    }

    // Return class name and hash instead of full toString which might contain sensitive data
    return response.getClass().getSimpleName() + "@" + Integer.toHexString(response.hashCode());
  }
}
