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
public class SmartAccountResponse {

  @JsonProperty("smartAccountAddress")
  private String smartAccountAddress;

  @JsonProperty("userAddress")
  private String userAddress;

  @JsonProperty("deployed")
  private Boolean deployed;

  @JsonProperty("message")
  private String message;

  @JsonProperty("bundlerUrl")
  private String bundlerUrl;

  @JsonProperty("paymasterUrl")
  private String paymasterUrl;

  @JsonProperty("chainId")
  private Integer chainId;

  @JsonProperty("usdcContract")
  private String usdcContract;
}
