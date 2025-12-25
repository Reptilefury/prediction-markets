package com.oregonmarkets.domain.user.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id private UUID id;

  @Column("email")
  private String email;

  @Column("username")
  private String username;

  @Column("display_name")
  private String displayName;

  // Magic.link Integration
  @Column("magic_user_id")
  private String magicUserId;

  @Column("magic_wallet_address")
  private String magicWalletAddress;

  @Column("magic_issuer")
  private String magicIssuer;

  // Web3 Wallet Integration
  @Column("web3_wallet_address")
  private String web3WalletAddress;

  @Column("auth_method")
  @Builder.Default
  private AuthMethod authMethod = AuthMethod.MAGIC;

  @Column("wallet_verified_at")
  private Instant walletVerifiedAt;

  // Enclave UDA Integration
  @Column("enclave_user_id")
  private String enclaveUserId;

  @Column("enclave_uda_address")
  private String enclaveUdaAddress;

  @Column("enclave_uda_tag")
  private String enclaveUdaTag;

  @Column("enclave_uda_created_at")
  private Instant enclaveUdaCreatedAt;

  @Column("enclave_uda_status")
  @Builder.Default
  private EnclaveUdaStatus enclaveUdaStatus = EnclaveUdaStatus.PENDING;

  @Column("enclave_deposit_addresses")
  private String enclaveDepositAddresses; // JSON serialized Map<String, Object> from Enclave API

  // Location
  @Column("country_code")
  private String countryCode;

  // Account Status
  @Column("is_active")
  @Builder.Default
  private Boolean isActive = true;

  @Column("email_verified")
  @Builder.Default
  private Boolean emailVerified = false;

  @Column("email_verified_at")
  private Instant emailVerifiedAt;

  // KYC
  @Column("kyc_status")
  @Builder.Default
  private KycStatus kycStatus = KycStatus.NOT_STARTED;

  @Column("kyc_level")
  @Builder.Default
  private Integer kycLevel = 0;

  // Trading Limits
  @Column("daily_deposit_limit")
  private BigDecimal dailyDepositLimit;

  @Column("daily_withdrawal_limit")
  private BigDecimal dailyWithdrawalLimit;

  // Blnk Integration
  @Column("blnk_identity_id")
  private String blnkIdentityId;

  @Column("blnk_balance_id")
  private String blnkBalanceId;

  @Column("blnk_created_at")
  private Instant blnkCreatedAt;

  // Polymarket Proxy Wallet Integration
  @Column("proxy_wallet_address")
  private String proxyWalletAddress;

  @Column("proxy_wallet_created_at")
  private Instant proxyWalletCreatedAt;

  @Column("proxy_wallet_status")
  @Builder.Default
  private ProxyWalletStatus proxyWalletStatus = ProxyWalletStatus.PENDING;

  // Biconomy Smart Account Integration (Account Abstraction)
  @Column("biconomy_smart_account_address")
  private String biconomySmartAccountAddress;

  @Column("biconomy_deployed")
  @Builder.Default
  private Boolean biconomyDeployed = false;

  @Column("biconomy_chain_id")
  @Builder.Default
  private Integer biconomyChainId = 137; // Polygon mainnet

  @Column("biconomy_bundler_url")
  private String biconomyBundlerUrl;

  @Column("biconomy_paymaster_url")
  private String biconomyPaymasterUrl;

  @Column("biconomy_created_at")
  private Instant biconomyCreatedAt;

  // Referral
  @Column("referral_code")
  private String referralCode;

  @Column("referred_by_user_id")
  private UUID referredByUserId;

  @Column("utm_source")
  private String utmSource;

  @Column("utm_medium")
  private String utmMedium;

  @Column("utm_campaign")
  private String utmCampaign;

  // Avatar and QR Codes
  @Column("avatar_url")
  private String avatarUrl;

  @Column("proxy_wallet_qr_code_url")
  private String proxyWalletQrCodeUrl;

  @Column("enclave_uda_qr_code_url")
  private String enclaveUdaQrCodeUrl;

  @Column("evm_deposit_qr_codes")
  private String evmDepositQrCodes;

  @Column("solana_deposit_qr_code_url")
  private String solanaDepositQrCodeUrl;

  @Column("bitcoin_deposit_qr_codes")
  private String bitcoinDepositQrCodes;

  // Timestamps
  @CreatedDate
  @Column("created_at")
  private Instant createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private Instant updatedAt;

  public enum EnclaveUdaStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    CLOSED
  }

  public enum KycStatus {
    NOT_STARTED,
    PENDING,
    APPROVED,
    REJECTED
  }

  public enum AuthMethod {
    MAGIC,
    WEB3_WALLET
  }

  public enum ProxyWalletStatus {
    PENDING,
    ACTIVE,
    FAILED,
    SUSPENDED
  }
}
