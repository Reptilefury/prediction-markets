package com.oregonMarkets.common.exception;

public class UserAlreadyExistsException extends BusinessException {
    
    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists");
    }
    
    public UserAlreadyExistsException(String field, String value) {
        super("User with " + field + " '" + value + "' already exists");
    }
}