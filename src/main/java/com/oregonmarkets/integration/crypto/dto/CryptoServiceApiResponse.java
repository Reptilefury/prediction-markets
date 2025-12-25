package com.oregonmarkets.integration.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoServiceApiResponse<T> {

  @JsonProperty("status")
  private String status;

  @JsonProperty("code")
  private Integer code;

  @JsonProperty("message")
  private String message;

  @JsonProperty("data")
  private T data;

  @JsonProperty("timestamp")
  private String timestamp;
}
