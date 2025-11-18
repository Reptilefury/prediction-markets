package com.oregonMarkets.common.exception;

public class BlockchainException extends BusinessException {
    
    public BlockchainException(String message) {
        super(message);
    }
    
    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
    }
}