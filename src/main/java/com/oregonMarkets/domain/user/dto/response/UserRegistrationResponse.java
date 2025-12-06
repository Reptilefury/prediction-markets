package com.oregonMarkets.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRegistrationResponse {

  private UUID userId;
  private String email;
  private String username;
  private String magicWalletAddress;
  private String enclaveUdaAddress;
  private String proxyWalletAddress;
  private DepositAddresses depositAddresses;
  private String referralCode;
  private String accessToken;
  private String refreshToken;
  private Instant createdAt;
  private String avatarUrl;
  private String proxyWalletQrCodeUrl;
  private String enclaveUdaQrCodeUrl;
  private String evmDepositQrCodes;
  private String solanaDepositQrCodeUrl;
  private String bitcoinDepositQrCodes;

  // Defensive copying for mutable fields
  public DepositAddresses getDepositAddresses() {
    return depositAddresses;
  }

  public void setDepositAddresses(DepositAddresses depositAddresses) {
    this.depositAddresses = depositAddresses;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DepositAddresses {
    private List<EVMDepositAddress> evmDepositAddress;
    private SolanaDepositAddress solanaDepositAddress;
    private BitcoinDepositAddress bitcoinDepositAddress;

    // Defensive copying for mutable fields
    public List<EVMDepositAddress> getEvmDepositAddress() {
      return evmDepositAddress == null ? null : new ArrayList<>(evmDepositAddress);
    }

    public void setEvmDepositAddress(List<EVMDepositAddress> evmDepositAddress) {
      this.evmDepositAddress =
          evmDepositAddress == null ? null : new ArrayList<>(evmDepositAddress);
    }

    public SolanaDepositAddress getSolanaDepositAddress() {
      return solanaDepositAddress;
    }

    public void setSolanaDepositAddress(SolanaDepositAddress solanaDepositAddress) {
      this.solanaDepositAddress = solanaDepositAddress;
    }

    public BitcoinDepositAddress getBitcoinDepositAddress() {
      return bitcoinDepositAddress;
    }

    public void setBitcoinDepositAddress(BitcoinDepositAddress bitcoinDepositAddress) {
      this.bitcoinDepositAddress = bitcoinDepositAddress;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class EVMDepositAddress {
    private Integer chainId;
    private String contractAddress;
    private Boolean deployed;
    private String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SolanaDepositAddress {
    private String address;
    private String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class BitcoinDepositAddress {
    private String legacyAddress;
    private String segwitAddress;
    private String nativeSegwitAddress;
    private String taprootAddress;
    private String id;
  }
}
