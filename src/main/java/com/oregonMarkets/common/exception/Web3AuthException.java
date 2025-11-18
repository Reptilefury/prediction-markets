package com.oregonMarkets.common.exception;

public class Web3AuthException extends BusinessException {
    
    public Web3AuthException(String message) {
        super(message);
    }
    
    public Web3AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}