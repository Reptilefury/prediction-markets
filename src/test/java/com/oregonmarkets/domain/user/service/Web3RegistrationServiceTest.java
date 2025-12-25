package com.oregonmarkets.domain.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.oregonmarkets.common.exception.UserAlreadyExistsException;
import com.oregonmarkets.common.exception.Web3AuthException;
import com.oregonmarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonmarkets.domain.user.model.User;
import com.oregonmarkets.domain.user.repository.UserRepository;
import com.oregonmarkets.integration.enclave.EnclaveClient;
import com.oregonmarkets.integration.web3.Web3AuthService;
import com.oregonmarkets.service.CacheService;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class Web3RegistrationServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private Web3AuthService web3AuthService;

  @Mock private EnclaveClient enclaveClient;

  @Mock private CacheService cacheService;

  @Mock private com.oregonmarkets.service.UsernameGenerationService usernameGenerationService;

  @Mock private com.oregonmarkets.integration.polymarket.ProxyWalletOnboardingService proxyWalletService;

  @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

  private Web3RegistrationService service;

  @BeforeEach
  void setUp() {
    service =
        new Web3RegistrationService(
            userRepository,
            web3AuthService,
            enclaveClient,
            cacheService,
            usernameGenerationService,
            proxyWalletService,
            eventPublisher);
    ReflectionTestUtils.setField(
        service, "destinationTokenAddress", "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174");
  }

  @Test
  void registerUser_Success() {
    Web3RegistrationRequest request = new Web3RegistrationRequest();
    request.setWalletAddress("0x123");
    request.setMessage("test message");
    request.setSignature("test signature");
    request.setCountryCode("US");

    when(web3AuthService.verifySignature(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(userRepository.existsByWeb3WalletAddress("0x123")).thenReturn(Mono.just(false));
    when(proxyWalletService.createUserProxyWallet(anyString()))
        .thenReturn(Mono.just("proxy-wallet-address"));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(cacheService.set(anyString(), any(), any(Duration.class))).thenReturn(Mono.empty());

    StepVerifier.create(service.registerUser(request))
        .expectNextMatches(
            response ->
                response.getUserId() != null
                    && response.getMagicWalletAddress().equals("0x123")
                    && response.getEnclaveUdaAddress() == null) // Will be null initially, populated async
        .verifyComplete();
  }

  @Test
  void registerUser_InvalidSignature() {
    Web3RegistrationRequest request = new Web3RegistrationRequest();
    request.setWalletAddress("0x123");
    request.setMessage("test message");
    request.setSignature("invalid signature");

    when(web3AuthService.verifySignature(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(false));

    StepVerifier.create(service.registerUser(request))
        .expectError(Web3AuthException.class)
        .verify();
  }

  @Test
  void registerUser_WalletAlreadyExists() {
    Web3RegistrationRequest request = new Web3RegistrationRequest();
    request.setWalletAddress("0x123");
    request.setMessage("test message");
    request.setSignature("test signature");

    when(web3AuthService.verifySignature(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(userRepository.existsByWeb3WalletAddress("0x123")).thenReturn(Mono.just(true));

    StepVerifier.create(service.registerUser(request))
        .expectError(UserAlreadyExistsException.class)
        .verify();
  }

  @Test
  void registerUser_WithReferralCode() {
    Web3RegistrationRequest request = new Web3RegistrationRequest();
    request.setWalletAddress("0x123");
    request.setMessage("test message");
    request.setSignature("test signature");
    request.setReferralCode("REF123");

    User referrer = new User();
    referrer.setId(UUID.randomUUID());

    when(web3AuthService.verifySignature(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(userRepository.existsByWeb3WalletAddress("0x123")).thenReturn(Mono.just(false));
    when(userRepository.findByReferralCode("REF123")).thenReturn(Mono.just(referrer));
    when(proxyWalletService.createUserProxyWallet(anyString()))
        .thenReturn(Mono.just("proxy-wallet-address"));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(cacheService.set(anyString(), any(), any(Duration.class))).thenReturn(Mono.empty());

    StepVerifier.create(service.registerUser(request))
        .expectNextMatches(response -> response.getUserId() != null)
        .verifyComplete();
  }

  @Test
  void registerUser_ProxyWalletCreationFails() {
    Web3RegistrationRequest request = new Web3RegistrationRequest();
    request.setWalletAddress("0x123");
    request.setMessage("test message");
    request.setSignature("test signature");

    when(web3AuthService.verifySignature(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(userRepository.existsByWeb3WalletAddress("0x123")).thenReturn(Mono.just(false));
    when(proxyWalletService.createUserProxyWallet(anyString()))
        .thenReturn(Mono.error(new RuntimeException("Proxy wallet error")));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(cacheService.set(anyString(), any(), any(Duration.class))).thenReturn(Mono.empty());

    StepVerifier.create(service.registerUser(request))
        .expectNextMatches(response -> response.getUserId() != null) // Should still succeed, just without proxy wallet
        .verifyComplete();
  }
}
