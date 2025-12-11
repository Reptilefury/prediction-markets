package com.oregonMarkets.integration.polymarket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.oregonMarkets.integration.crypto.CryptoServiceClient;
import com.oregonMarkets.integration.crypto.dto.SmartAccountResponse;
import com.oregonMarkets.integration.crypto.dto.WalletCreateResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProxyWalletOnboardingServiceTest {

  @Mock private CryptoServiceClient cryptoServiceClient;

  private ProxyWalletOnboardingService service;

  @BeforeEach
  void setUp() {
    service = new ProxyWalletOnboardingService(cryptoServiceClient);
    ReflectionTestUtils.setField(
        service, "proxyFactoryAddress", "0xaB45c5A4B0c941a2F231C04C3f49182e1A254052");
  }

  @Test
  void createUserProxyWallet_Success() {
    String userEOA = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";
    String didToken = "mock-did-token";
    String smartAccountAddress = "0x1234567890123456789012345678901234567890";

    WalletCreateResponseData mockResponse =
        WalletCreateResponseData.builder()
            .user(
                WalletCreateResponseData.UserInfo.builder()
                    .id("user123")
                    .walletAddress(userEOA)
                    .build())
            .smartAccount(
                SmartAccountResponse.builder()
                    .smartAccountAddress(smartAccountAddress)
                    .userAddress(userEOA)
                    .deployed(false)
                    .chainId(137)
                    .bundlerUrl("https://bundler.example.com")
                    .paymasterUrl("https://paymaster.example.com")
                    .usdcContract("0xUSDC")
                    .build())
            .build();

    when(cryptoServiceClient.createSmartAccount(anyString(), anyString()))
        .thenReturn(Mono.just(mockResponse));

    StepVerifier.create(service.createUserProxyWallet(userEOA, didToken))
        .expectNextMatches(
            response ->
                response.getSmartAccount().getSmartAccountAddress().equals(smartAccountAddress)
                    && response.getSmartAccount().getChainId() == 137)
        .verifyComplete();
  }

  @Test
  void createUserProxyWallet_NullAddress() {
    String didToken = "mock-did-token";

    StepVerifier.create(service.createUserProxyWallet(null, didToken))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void createUserProxyWallet_EmptyAddress() {
    String didToken = "mock-did-token";

    StepVerifier.create(service.createUserProxyWallet("", didToken))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void createUserProxyWallet_NullDidToken() {
    String userEOA = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

    StepVerifier.create(service.createUserProxyWallet(userEOA, null))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void createUserProxyWallet_EmptyDidToken() {
    String userEOA = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

    StepVerifier.create(service.createUserProxyWallet(userEOA, ""))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void createUserProxyWallet_Deprecated() {
    String userEOA = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

    StepVerifier.create(service.createUserProxyWallet(userEOA))
        .expectError(UnsupportedOperationException.class)
        .verify();
  }

  @Test
  void isValidProxyWalletAddress_Valid() {
    String validAddress = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

    StepVerifier.create(service.isValidProxyWalletAddress(validAddress))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void isValidProxyWalletAddress_Invalid() {
    StepVerifier.create(service.isValidProxyWalletAddress("invalid"))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void isValidProxyWalletAddress_Null() {
    StepVerifier.create(service.isValidProxyWalletAddress(null)).expectNext(false).verifyComplete();
  }

  @Test
  void getProxyWalletCreationInfo() {
    String info = service.getProxyWalletCreationInfo();

    assertTrue(info.contains("Biconomy smart account"));
    assertTrue(info.contains("crypto-service"));
  }
}
