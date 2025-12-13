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
  private String displayName;
  
  // Magic.link Integration
  private String magicWalletAddress;
  private String magicIssuer;
  
  // Web3 Wallet Integration
  private String web3WalletAddress;
  private String authMethod;
  private Instant walletVerifiedAt;
  
  // Enclave UDA Integration
  private String enclaveUserId;
  private String enclaveUdaAddress;
  private String enclaveUdaTag;
  private Instant enclaveUdaCreatedAt;
  private String enclaveUdaStatus;
  private Object enclaveDepositAddresses;
  
  // Account Status
  private String countryCode;
  private Boolean isActive;
  private Boolean emailVerified;
  private Instant emailVerifiedAt;
  
  // KYC
  private String kycStatus;
  private Integer kycLevel;
  
  // Trading Limits
  private String dailyDepositLimit;
  private String dailyWithdrawalLimit;
  
  // Blnk Integration
  private String blnkIdentityId;
  private String blnkBalanceId;
  private Instant blnkCreatedAt;
  
  // Polymarket Proxy Wallet Integration
  private String proxyWalletAddress;
  private Instant proxyWalletCreatedAt;
  private String proxyWalletStatus;
  
  // Biconomy Smart Account Integration
  private String biconomySmartAccountAddress;
  private Boolean biconomyDeployed;
  private Integer biconomyChainId;
  private String biconomyBundlerUrl;
  private String biconomyPaymasterUrl;
  private Instant biconomyCreatedAt;
  
  // Referral
  private String referralCode;
  private UUID referredByUserId;
  private String utmSource;
  private String utmMedium;
  private String utmCampaign;
  
  // Avatar and QR Codes
  private String avatarUrl;
  private String proxyWalletQrCodeUrl;
  private String enclaveUdaQrCodeUrl;
  private Object evmDepositQrCodes;
  private String solanaDepositQrCodeUrl;
  private Object bitcoinDepositQrCodes;
  
  // Tokens
  private String accessToken;
  private String refreshToken;

  private DepositAddresses depositAddresses;

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
