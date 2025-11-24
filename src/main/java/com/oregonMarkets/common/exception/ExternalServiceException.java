package com.oregonMarkets.common.exception;

import com.oregonMarkets.common.response.ResponseCode;
import lombok.Getter;

/**
 * Exception for external service integration errors
 * Used for blockchain, payment providers, wallet services, etc.
 */
@Getter
public class ExternalServiceException extends RuntimeException {
    private final ResponseCode responseCode;
    private final String serviceName;
    private final String details;

    public ExternalServiceException(ResponseCode responseCode, String serviceName) {
        super(responseCode.getMessage() + " - Service: " + serviceName);
        this.responseCode = responseCode;
        this.serviceName = serviceName;
        this.details = null;
    }

    public ExternalServiceException(ResponseCode responseCode, String serviceName, String message) {
        super(message);
        this.responseCode = responseCode;
        this.serviceName = serviceName;
        this.details = null;
    }

    public ExternalServiceException(ResponseCode responseCode, String serviceName, String message, String details) {
        super(message);
        this.responseCode = responseCode;
        this.serviceName = serviceName;
        this.details = details;
    }

    public ExternalServiceException(ResponseCode responseCode, String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
        this.serviceName = serviceName;
        this.details = cause != null ? cause.getMessage() : null;
    }

    /**
     * Create exception for blockchain errors
     */
    public static ExternalServiceException blockchain(String message) {
        return new ExternalServiceException(ResponseCode.BLOCKCHAIN_ERROR, "Blockchain", message);
    }

    /**
     * Create exception for Blnk integration errors
     */
    public static ExternalServiceException blnk(String message) {
        return new ExternalServiceException(ResponseCode.EXTERNAL_SERVICE_ERROR, "Blnk Core", message);
    }

    /**
     * Create exception for Polymarket proxy wallet errors
     */
    public static ExternalServiceException polymarket(String message) {
        return new ExternalServiceException(ResponseCode.EXTERNAL_SERVICE_ERROR, "Polymarket", message);
    }
}
