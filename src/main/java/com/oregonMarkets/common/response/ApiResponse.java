package com.oregonMarkets.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized API response wrapper for Oregon Markets
 * Ensures consistent response structure across all endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private ResponseStatus status;
    private String message;
    private int code;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private T data;

    private Map<String, Object> metadata;

    private ErrorDetails error;

    // Defensive copying for mutable fields
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : new HashMap<>(metadata);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? null : new HashMap<>(metadata);
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    /**
     * Create a success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .message(ResponseCode.SUCCESS.getMessage())
                .code(ResponseCode.SUCCESS.getCode())
                .data(data)
                .build();
    }

    /**
     * Create a success response with custom response code
     */
    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .message(responseCode.getMessage())
                .code(responseCode.getCode())
                .data(data)
                .build();
    }

    /**
     * Create a success response with custom message
     */
    public static <T> ApiResponse<T> success(ResponseCode responseCode, String customMessage, T data) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .message(customMessage)
                .code(responseCode.getCode())
                .data(data)
                .build();
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.FAILED)
                .message(responseCode.getMessage())
                .code(responseCode.getCode())
                .error(ErrorDetails.builder()
                        .code(responseCode.getCode())
                        .message(responseCode.getMessage())
                        .build())
                .build();
    }

    /**
     * Create an error response with custom message
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String customMessage) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.FAILED)
                .message(customMessage)
                .code(responseCode.getCode())
                .error(ErrorDetails.builder()
                        .code(responseCode.getCode())
                        .message(customMessage)
                        .build())
                .build();
    }


    /**
     * Create an error response with details
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String customMessage, String details) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.FAILED)
                .message(customMessage)
                .code(responseCode.getCode())
                .error(ErrorDetails.builder()
                        .code(responseCode.getCode())
                        .message(customMessage)
                        .details(details)
                        .build())
                .build();
    }

    /**
     * Create an error response with validation errors
     */
    public static <T> ApiResponse<T> validationError(Map<String, String> validationErrors) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.FAILED)
                .message(ResponseCode.VALIDATION_ERROR.getMessage())
                .code(ResponseCode.VALIDATION_ERROR.getCode())
                .error(ErrorDetails.builder()
                        .code(ResponseCode.VALIDATION_ERROR.getCode())
                        .message(ResponseCode.VALIDATION_ERROR.getMessage())
                        .validationErrors(validationErrors)
                        .build())
                .build();
    }

    /**
     * Add metadata to the response
     */
    public ApiResponse<T> withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Add multiple metadata entries
     */
    public ApiResponse<T> withMetadata(Map<String, Object> metadata) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.putAll(metadata);
        return this;
    }

    /**
     * Convert to ResponseEntity with appropriate HTTP status
     */
    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        ResponseCode responseCode = ResponseCode.fromCode(this.code);
        return ResponseEntity
                .status(responseCode.getHttpStatus())
                .body(this);
    }

    /**
     * Convert to ResponseEntity with custom HTTP status
     */
    public ResponseEntity<ApiResponse<T>> toResponseEntity(HttpStatus httpStatus) {
        return ResponseEntity
                .status(httpStatus)
                .body(this);
    }

    /**
     * Error details structure
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private int code;
        private String message;
        private String details;
        private String field;
        private Map<String, String> validationErrors;
        private String traceId;

        // Defensive copying for mutable fields
        public Map<String, String> getValidationErrors() {
            return validationErrors == null ? null : new HashMap<>(validationErrors);
        }

        public void setValidationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors == null ? null : new HashMap<>(validationErrors);
        }
    }
}
