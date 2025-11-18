package com.oregonMarkets.common.exception;

public class BlnkApiException extends BusinessException {
    
    public BlnkApiException(String message) {
        super("Blnk API error: " + message);
    }
    
    public BlnkApiException(String message, Throwable cause) {
        super("Blnk API error: " + message, cause);
    }
}