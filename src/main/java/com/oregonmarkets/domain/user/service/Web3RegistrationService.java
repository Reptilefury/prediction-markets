package com.oregonmarkets.domain.user.service;

import com.oregonmarkets.common.exception.UserAlreadyExistsException;
import com.oregonmarkets.common.exception.Web3AuthException;
import com.oregonmarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonmarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonmarkets.domain.user.model.User;
import com.oregonmarkets.domain.user.repository.UserRepository;
import com.oregonmarkets.event.UserRegisteredEvent;
import com.oregonmarkets.event.ProxyWalletCreatedEvent;
import com.oregonmarkets.integration.enclave.EnclaveClient;
import com.oregonmarkets.integration.web3.Web3AuthService;
import com.oregonmarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonmarkets.service.CacheService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class Web3RegistrationService {

  private final UserRepository userRepository;
  private final Web3AuthService web3AuthService;
  private final EnclaveClient enclaveClient;
  private final CacheService cacheService;
  private final com.oregonmarkets.service.UsernameGenerationService usernameGenerationService;
  private final ProxyWalletOnboardingService proxyWalletService;
  private final org.springframework.context.ApplicationEventPublisher eventPublisher;

  @Value("${app.enclave.destination-token-address:0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174}")
  private String destinationTokenAddress;

  public Mono<UserRegistrationResponse> registerUser(Web3RegistrationRequest request) {
    log.info("Starting Web3 user registration for wallet: {}", request.getWalletAddress());

    return web3AuthService
        .verifySignature(request.getWalletAddress(), request.getMessage(), request.getSignature())
        .flatMap(
            isValid -> {
              if (!isValid) {
                return Mono.error(new Web3AuthException("Invalid wallet signature"));
              }
              return checkWalletExists(request.getWalletAddress())
                  .then(createWeb3User(request))
                  .flatMap(userRepository::save) // Save first to generate UUID
                  .flatMap(user -> setupExternalIntegrationsAsyncViaEvents(user)) // Setup proxy wallet and async chain
                  .flatMap(
                      savedUser ->
                          publishUserRegisteredEvent(savedUser)
                              .then(cacheUserData(savedUser))
                              .thenReturn(buildResponse(savedUser))); // Return immediately, rest happens async
            })
        .doOnSuccess(
            response -> log.info("Successfully registered Web3 user: {}", response.getUserId()))
        .doOnError(error -> log.error("Failed to register Web3 user: {}", 
            error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName(), error));
  }

  private Mono<Void> checkWalletExists(String walletAddress) {
    return userRepository
        .existsByWeb3WalletAddress(walletAddress)
        .flatMap(
            exists ->
                exists
                    ? Mono.error(new UserAlreadyExistsException("wallet address", walletAddress))
                    : Mono.empty());
  }

  private Mono<User> createWeb3User(Web3RegistrationRequest request) {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .web3WalletAddress(request.getWalletAddress())
            .authMethod(User.AuthMethod.WEB3_WALLET)
            .walletVerifiedAt(Instant.now())
            .countryCode(request.getCountryCode())
            .kycStatus(User.KycStatus.NOT_STARTED)
            .kycLevel(0)
            .enclaveUdaStatus(User.EnclaveUdaStatus.PENDING)
            .referralCode(generateReferralCode())
            .utmSource(request.getUtmSource())
            .utmMedium(request.getUtmMedium())
            .utmCampaign(request.getUtmCampaign())
            .build();

    // Generate username and display name using Datafaker with UUID uniqueness
    usernameGenerationService.applyUsernameAndDisplayName(user);

    if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
      return userRepository
          .findByReferralCode(request.getReferralCode())
          .map(
              referrer -> {
                user.setReferredByUserId(referrer.getId());
                return user;
              })
          .switchIfEmpty(Mono.just(user));
    }

    return Mono.just(user);
  }

  /**
   * Setup external integrations asynchronously via events.
   * Flow: ProxyWallet → ProxyWalletCreatedEvent → EnclaveUdaCreationListener → EnclaveUdaCreatedEvent → BlnkBalanceCreationListener
   */
  private Mono<User> setupExternalIntegrationsAsyncViaEvents(User user) {
    // Create proxy wallet synchronously (needed for event data)
    return proxyWalletService
        .createUserProxyWallet(user.getWeb3WalletAddress())
        .map(
            proxyWalletAddress -> {
              user.setProxyWalletAddress(proxyWalletAddress);
              user.setProxyWalletStatus(User.ProxyWalletStatus.ACTIVE);
              user.setProxyWalletCreatedAt(Instant.now());
              return user;
            })
        .onErrorResume(
            error -> {
              log.warn("Failed to create proxy wallet, continuing without it: {}", error.getMessage());
              return Mono.just(user);
            })
        .flatMap(u -> userRepository.save(u)) // Save updated user with proxy wallet
        .doOnSuccess(
            u -> {
              // Publish ProxyWalletCreatedEvent to kickoff the async event chain
              ProxyWalletCreatedEvent event =
                  ProxyWalletCreatedEvent.builder()
                      .userId(u.getId())
                      .magicWalletAddress(u.getWeb3WalletAddress()) // Use Web3 wallet as magic wallet
                      .proxyWalletAddress(u.getProxyWalletAddress())
                      .email(u.getEmail())
                      .magicUserId(u.getId().toString()) // Use user ID as magic user ID for Web3
                      .didToken("web3-wallet-" + u.getWeb3WalletAddress()) // Generate token for Web3
                      .timestamp(Instant.now())
                      .build();
              eventPublisher.publishEvent(event);
              log.info("Published ProxyWalletCreatedEvent for Web3 user: {}, starting async chain", u.getId());
            })
        .doOnError(error -> log.error("Error setting up external integrations async: {}", error.getMessage()));
  }

  private UserRegistrationResponse buildResponse(User user) {
    return UserRegistrationResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .magicWalletAddress(user.getWeb3WalletAddress()) // Use Web3 wallet
        .enclaveUdaAddress(user.getEnclaveUdaAddress()) // Will be null initially, populated async
        .depositAddresses(null) // Will be populated async via events
        .referralCode(user.getReferralCode())
        .accessToken("mock-access-token")
        .refreshToken("mock-refresh-token")
        .build();
  }

  private Mono<Void> publishUserRegisteredEvent(User user) {
    return Mono.fromRunnable(
        () -> {
          UserRegisteredEvent event =
              UserRegisteredEvent.from(
                  user.getId(),
                  user.getEmail(),
                  user.getWeb3WalletAddress(),
                  user.getEnclaveUdaAddress(),
                  user.getReferralCode(),
                  user.getReferredByUserId());
        });
  }

  private Mono<Void> cacheUserData(User user) {
    String cacheKey = "user:" + user.getId();
    return cacheService
        .set(cacheKey, user, java.time.Duration.ofHours(1))
        .onErrorResume(
            error -> {
              log.warn("Failed to cache user data: {}", error.getMessage());
              return Mono.empty();
            });
  }

  private String generateReferralCode() {
    return "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
