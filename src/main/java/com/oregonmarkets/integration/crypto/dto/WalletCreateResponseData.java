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
public class WalletCreateResponseData {

  @JsonProperty("user")
  private UserInfo user;

  @JsonProperty("smartAccount")
  private SmartAccountResponse smartAccount;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("walletAddress")
    private String walletAddress;
  }
}
