package com.oregonMarkets.common.exception;

public class MagicAuthException extends BusinessException {
    
    public MagicAuthException(String message) {
        super("Magic authentication failed: " + message);
    }
    
    public MagicAuthException(String message, Throwable cause) {
        super("Magic authentication failed: " + message, cause);
    }
}