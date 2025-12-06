package com.oregonMarkets;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonMarkets.domain.blockchain.model.BlockchainChain;
import com.oregonMarkets.domain.enclave.model.EnclaveChainAddress;
import com.oregonMarkets.domain.payment.model.Deposit;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.dto.ErrorResponse;
import com.oregonMarkets.event.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AllBuildersTest {

  @Test
  void blockchainChain() {
    BlockchainChain c =
        BlockchainChain.builder()
            .id(UUID.randomUUID())
            .chainName("n")
            .chainId(1)
            .rpcUrl("u")
            .usdcTokenAddress("a")
            .usdcDecimals(6)
            .requiredConfirmations(12)
            .isActive(true)
            .lastScannedBlock(0L)
            .createdAt(Instant.now())
            .build();
    assertNotNull(c);
  }

  @Test
  void enclaveChainAddress() {
    EnclaveChainAddress a =
        EnclaveChainAddress.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .chainType(EnclaveChainAddress.ChainType.ETHEREUM)
            .depositAddress("a")
            .createdAt(Instant.now())
            .build();
    assertNotNull(a);
  }

  @Test
  void deposit() {
    Deposit d =
        Deposit.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .amount(BigDecimal.TEN)
            .currency("U")
            .method(Deposit.DepositMethod.CRYPTO)
            .status(Deposit.DepositStatus.PENDING)
            .processingStatus(Deposit.ProcessingStatus.DETECTED)
            .txHash("h")
            .chainId(1)
            .toAddress("a")
            .tokenAddress("t")
            .rawAmount("r")
            .blockNumber(1L)
            .confirmations(0)
            .requiredConfirmations(12)
            .createdAt(Instant.now())
            .build();
    assertNotNull(d);
  }

  @Test
  void user() {
    User u =
        User.builder()
            .id(UUID.randomUUID())
            .email("e")
            .username("u")
            .displayName("d")
            .magicUserId("m")
            .magicWalletAddress("a")
            .magicIssuer("i")
            .web3WalletAddress("w")
            .authMethod(User.AuthMethod.MAGIC)
            .walletVerifiedAt(Instant.now())
            .enclaveUserId("e")
            .enclaveUdaAddress("u")
            .enclaveUdaTag("t")
            .enclaveUdaCreatedAt(Instant.now())
            .enclaveUdaStatus(User.EnclaveUdaStatus.ACTIVE)
            .enclaveDepositAddresses("d")
            .countryCode("US")
            .isActive(true)
            .emailVerified(true)
            .emailVerifiedAt(Instant.now())
            .kycStatus(User.KycStatus.APPROVED)
            .kycLevel(1)
            .dailyDepositLimit(BigDecimal.TEN)
            .dailyWithdrawalLimit(BigDecimal.ONE)
            .blnkIdentityId("b")
            .blnkBalanceId("b")
            .blnkCreatedAt(Instant.now())
            .proxyWalletAddress("p")
            .proxyWalletCreatedAt(Instant.now())
            .proxyWalletStatus(User.ProxyWalletStatus.ACTIVE)
            .referralCode("r")
            .referredByUserId(UUID.randomUUID())
            .utmSource("s")
            .utmMedium("m")
            .utmCampaign("c")
            .avatarUrl("a")
            .proxyWalletQrCodeUrl("p")
            .enclaveUdaQrCodeUrl("e")
            .evmDepositQrCodes("e")
            .solanaDepositQrCodeUrl("s")
            .bitcoinDepositQrCodes("b")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    assertNotNull(u);
  }

  @Test
  void errorResponse() {
    ErrorResponse e =
        ErrorResponse.builder()
            .status("s")
            .errorMessage("m")
            .statusCode("c")
            .errorCode("e")
            .timestamp(Instant.now())
            .build();
    assertNotNull(e);
  }

  @Test
  void assetsGenerationEvent() {
    AssetsGenerationEvent e =
        AssetsGenerationEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("m")
            .proxyWalletAddress("p")
            .enclaveUdaAddress("e")
            .depositAddresses(new HashMap<>())
            .timestamp(Instant.now())
            .build();
    assertNotNull(e);
  }

  @Test
  void blnkBalanceCreatedEvent() {
    BlnkBalanceCreatedEvent e =
        BlnkBalanceCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("m")
            .proxyWalletAddress("p")
            .enclaveUdaAddress("e")
            .blnkIdentityId("i")
            .blnkBalanceId("b")
            .email("e")
            .magicUserId("m")
            .didToken("d")
            .timestamp(Instant.now())
            .build();
    assertNotNull(e);
  }

  @Test
  void enclaveUdaCreatedEvent() {
    EnclaveUdaCreatedEvent e =
        EnclaveUdaCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("m")
            .proxyWalletAddress("p")
            .enclaveUdaAddress("e")
            .email("e")
            .magicUserId("m")
            .didToken("d")
            .timestamp(Instant.now())
            .build();
    assertNotNull(e);
  }

  @Test
  void keycloakProvisionEvent() {
    KeycloakProvisionEvent e =
        KeycloakProvisionEvent.builder()
            .userId(UUID.randomUUID())
            .username("u")
            .initialPassword("p")
            .timestamp(Instant.now())
            .build();
    assertNotNull(e);
  }

  @Test
  void proxyWalletCreatedEvent() {
    ProxyWalletCreatedEvent e =
        ProxyWalletCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("m")
            .proxyWalletAddress("p")
            .email("e")
            .magicUserId("m")
            .didToken("d")
            .timestamp(Instant.now())
            .build();
    assertNotNull(e);
  }

  @Test
  void userRegistrationResponse() {
    UserRegistrationResponse r =
        UserRegistrationResponse.builder()
            .userId(UUID.randomUUID())
            .email("e")
            .username("u")
            .magicWalletAddress("m")
            .enclaveUdaAddress("e")
            .proxyWalletAddress("p")
            .depositAddresses(null)
            .referralCode("r")
            .accessToken("a")
            .refreshToken("r")
            .createdAt(Instant.now())
            .avatarUrl("a")
            .proxyWalletQrCodeUrl("p")
            .enclaveUdaQrCodeUrl("e")
            .evmDepositQrCodes("e")
            .solanaDepositQrCodeUrl("s")
            .bitcoinDepositQrCodes("b")
            .build();
    assertNotNull(r);
  }

  @Test
  void evmDepositAddress() {
    UserRegistrationResponse.EVMDepositAddress a =
        UserRegistrationResponse.EVMDepositAddress.builder()
            .chainId(1)
            .contractAddress("c")
            .deployed(true)
            .id("i")
            .build();
    assertNotNull(a);
  }

  @Test
  void solanaDepositAddress() {
    UserRegistrationResponse.SolanaDepositAddress a =
        UserRegistrationResponse.SolanaDepositAddress.builder().address("a").id("i").build();
    assertNotNull(a);
  }

  @Test
  void bitcoinDepositAddress() {
    UserRegistrationResponse.BitcoinDepositAddress a =
        UserRegistrationResponse.BitcoinDepositAddress.builder()
            .legacyAddress("l")
            .segwitAddress("s")
            .nativeSegwitAddress("n")
            .taprootAddress("t")
            .id("i")
            .build();
    assertNotNull(a);
  }

  @Test
  void depositAddresses() {
    UserRegistrationResponse.DepositAddresses d =
        UserRegistrationResponse.DepositAddresses.builder()
            .evmDepositAddress(new ArrayList<>())
            .solanaDepositAddress(null)
            .bitcoinDepositAddress(null)
            .build();
    assertNotNull(d);
  }
}
