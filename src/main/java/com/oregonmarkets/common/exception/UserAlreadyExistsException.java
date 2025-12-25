package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class UserAlreadyExistsException extends BusinessException {

  public UserAlreadyExistsException(String email) {
    super(ResponseCode.DUPLICATE_USER, "User with email '" + email + "' already exists");
  }

  public UserAlreadyExistsException(String field, String value) {
    super(ResponseCode.DUPLICATE_USER, "User with " + field + " '" + value + "' already exists");
  }
}
