package com.oregonMarkets.domain.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.integration.blnk.BlnkClient;
import com.oregonMarkets.integration.crypto.dto.SmartAccountResponse;
import com.oregonMarkets.integration.crypto.dto.WalletCreateResponseData;
import com.oregonMarkets.integration.enclave.EnclaveClient;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import com.oregonMarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonMarkets.service.CacheService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private EnclaveClient enclaveClient;

  @Mock private BlnkClient blnkClient;

  @Mock private ProxyWalletOnboardingService proxyWalletOnboardingService;

  @Mock private CacheService cacheService;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private com.oregonMarkets.service.UsernameGenerationService usernameGenerationService;

  private UserRegistrationService userRegistrationService;

  @BeforeEach
  void setUp() {
    userRegistrationService =
        new UserRegistrationService(
            userRepository,
            proxyWalletOnboardingService,
            cacheService,
            eventPublisher,
            usernameGenerationService);
  }

  @Test
  void registerUser_Success() {
    // Given
    UserRegistrationRequest request = createValidRequest();
    MagicDIDValidator.MagicUserInfo magicUser = createMagicUserInfo();
    String didToken = "test-did-token";
    User savedUser = createSavedUser();

    // Mock crypto-service smart account response
    WalletCreateResponseData walletResponse =
        WalletCreateResponseData.builder()
            .user(
                WalletCreateResponseData.UserInfo.builder()
                    .id("user123")
                    .walletAddress("0x123")
                    .build())
            .smartAccount(
                SmartAccountResponse.builder()
                    .smartAccountAddress("0x456")
                    .userAddress("0x123")
                    .deployed(false)
                    .chainId(137)
                    .bundlerUrl("https://bundler.example.com")
                    .paymasterUrl("https://paymaster.example.com")
                    .usdcContract("0xUSDC")
                    .build())
            .build();

    // When
    when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
    when(userRepository.existsByMagicUserId(anyString())).thenReturn(Mono.just(false));
    when(proxyWalletOnboardingService.createUserProxyWallet(anyString(), anyString()))
        .thenReturn(Mono.just(walletResponse));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
    when(cacheService.set(anyString(), any(), any())).thenReturn(Mono.empty());

    // Then
    StepVerifier.create(userRegistrationService.registerUser(request, magicUser, didToken))
        .expectNextMatches(
            response ->
                response.getEmail().equals("test@example.com")
                    && response.getMagicWalletAddress().equals("0x123")
                    && response.getEnclaveUdaAddress().equals("0xuda123"))
        .verifyComplete();
  }

  @Test
  void registerUser_UserAlreadyExists() {
    // Given
    UserRegistrationRequest request = createValidRequest();
    MagicDIDValidator.MagicUserInfo magicUser = createMagicUserInfo();
    String didToken = "test-did-token";

    // When
    when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));
    when(userRepository.existsByMagicUserId(anyString())).thenReturn(Mono.just(false));

    // Then
    StepVerifier.create(userRegistrationService.registerUser(request, magicUser, didToken))
        .expectErrorMatches(
            throwable ->
                throwable instanceof UserAlreadyExistsException
                    && throwable.getMessage().contains("test@example.com"))
        .verify();
  }

  private UserRegistrationRequest createValidRequest() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("test@example.com");
    request.setCountryCode("US");
    return request;
  }

  private MagicDIDValidator.MagicUserInfo createMagicUserInfo() {
    MagicDIDValidator.MagicUserInfo magicUser = new MagicDIDValidator.MagicUserInfo();
    magicUser.setEmail("test@example.com");
    magicUser.setIssuer("test-issuer");
    magicUser.setPublicAddress("0x123");
    magicUser.setUserId("test-user-id");
    return magicUser;
  }

  private User createSavedUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .magicUserId("test-issuer")
        .magicWalletAddress("0x123")
        .enclaveUdaAddress("0xuda123")
        .referralCode("REF12345678")
        .createdAt(Instant.now())
        .build();
  }
}
