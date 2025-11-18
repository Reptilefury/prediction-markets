package com.oregonMarkets.common.exception;

public class EnclaveApiException extends BusinessException {
    
    public EnclaveApiException(String message) {
        super("Enclave API error: " + message);
    }
    
    public EnclaveApiException(String message, Throwable cause) {
        super("Enclave API error: " + message, cause);
    }
}