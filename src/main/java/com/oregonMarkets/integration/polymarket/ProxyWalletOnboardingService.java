package com.oregonMarkets.integration.polymarket;

import com.oregonMarkets.common.exception.ExternalServiceException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.common.util.DataMaskingUtil;
import com.oregonMarkets.integration.crypto.CryptoServiceClient;
import com.oregonMarkets.integration.crypto.dto.WalletCreateResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for managing proxy wallets (smart accounts) for users
 *
 * <p>Now uses Biconomy Account Abstraction (AA) for smart account creation
 * via the crypto-service microservice.
 *
 * <p>Key Architecture:
 * - Biconomy Smart Accounts (v2) for gasless transactions
 * - Account Abstraction (ERC-4337) for improved UX
 * - Deterministic address generation
 * - Sponsored transactions via Biconomy Paymaster
 *
 * <p>Flow:
 * 1. User registers -> call crypto-service to create smart account
 * 2. Store smart account address and metadata in database
 * 3. Smart account deploys on first transaction (lazy deployment)
 * 4. All transactions are gasless via Biconomy bundler
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyWalletOnboardingService {

  @Value("${app.polymarket.proxy-factory-address:0xaB45c5A4B0c941a2F231C04C3f49182e1A254052}")
  private String proxyFactoryAddress;

  private final CryptoServiceClient cryptoServiceClient;

  /**
   * Create a Biconomy smart account for a user via crypto-service
   *
   * @param userEOA the user's Ethereum address (from Magic DID token)
   * @param didToken the user's Magic DID token for authentication
   * @return Mono containing the smart account data including address
   * @throws ExternalServiceException if smart account creation fails
   */
  public Mono<WalletCreateResponseData> createUserProxyWallet(String userEOA, String didToken) {
    log.info("[PROXY-WALLET] Initiating Biconomy smart account creation");
    log.info("[PROXY-WALLET] User EOA: {}", DataMaskingUtil.maskWalletAddress(userEOA));

    // Validate inputs
    if (userEOA == null || userEOA.trim().isEmpty()) {
      log.error("[PROXY-WALLET] Validation failed: User EOA address is null or empty");
      return Mono.error(
          new ExternalServiceException(
              ResponseCode.VALIDATION_ERROR,
              "Proxy Wallet Service",
              "User EOA address cannot be null or empty"));
    }

    if (didToken == null || didToken.trim().isEmpty()) {
      log.error("[PROXY-WALLET] Validation failed: DID token is null or empty");
      return Mono.error(
          new ExternalServiceException(
              ResponseCode.VALIDATION_ERROR,
              "Proxy Wallet Service",
              "DID token cannot be null or empty"));
    }

    // Normalize the address
    String normalizedEOA =
        userEOA.toLowerCase().startsWith("0x")
            ? userEOA.toLowerCase()
            : "0x" + userEOA.toLowerCase();

    log.info("[PROXY-WALLET] Normalized EOA address: {}", DataMaskingUtil.maskWalletAddress(normalizedEOA));
    log.info("[PROXY-WALLET] Calling crypto-service to create smart account...");

    return cryptoServiceClient
        .createSmartAccount(normalizedEOA, didToken)
        .doOnSuccess(
            response -> {
              log.info("[PROXY-WALLET] ✓ Smart account creation successful");
              log.info(
                  "[PROXY-WALLET] Smart Account Address: {}",
                  DataMaskingUtil.maskWalletAddress(response.getSmartAccount().getSmartAccountAddress()));
              log.debug(
                  "[PROXY-WALLET] User EOA mapped to Smart Account successfully");
              log.info(
                  "[PROXY-WALLET] Deployment status: {} (will deploy on first transaction)",
                  response.getSmartAccount().getDeployed() ? "Already deployed" : "Lazy deployment");
            })
        .doOnError(
            error -> {
              log.error("[PROXY-WALLET] ✗ Smart account creation failed for user EOA: {}",
                  DataMaskingUtil.maskWalletAddress(userEOA));
              log.error("[PROXY-WALLET] Error type: {}", error.getClass().getSimpleName());
              log.error("[PROXY-WALLET] Error message: {}", error.getMessage());
              if (error.getCause() != null) {
                log.error("[PROXY-WALLET] Root cause: {}", error.getCause().getMessage());
              }
            })
        .onErrorMap(
            throwable -> {
              // If already an ExternalServiceException, return as-is
              if (throwable instanceof ExternalServiceException) {
                return throwable;
              }
              // Wrap other exceptions
              log.error("[PROXY-WALLET] Wrapping unexpected error as ExternalServiceException");
              return new ExternalServiceException(
                  ResponseCode.EXTERNAL_SERVICE_ERROR,
                  "Proxy Wallet Service",
                  "Failed to create smart account for user: " + throwable.getMessage(),
                  throwable);
            });
  }

  /**
   * Overloaded method for backward compatibility with existing code
   * that doesn't pass didToken
   *
   * @deprecated Use {@link #createUserProxyWallet(String, String)} instead
   */
  @Deprecated
  public Mono<String> createUserProxyWallet(String userEOA) {
    log.warn(
        "createUserProxyWallet called without didToken - this method is deprecated and will return error");
    return Mono.error(
        new UnsupportedOperationException(
            "Smart account creation now requires DID token. Use createUserProxyWallet(userEOA, didToken)"));
  }

  /**
   * Get information about smart account creation
   *
   * @return information message
   */
  public String getProxyWalletCreationInfo() {
    return "Biconomy smart account is created via crypto-service. "
        + "The smart account will be deployed on-chain on the first transaction (lazy deployment). "
        + "All transactions are gasless via Biconomy's paymaster.";
  }

  /**
   * Verify the proxy wallet address is valid
   *
   * @param proxyWalletAddress the proxy wallet address to verify
   * @return Mono<Boolean> true if address is valid Ethereum format
   */
  public Mono<Boolean> isValidProxyWalletAddress(String proxyWalletAddress) {
    if (proxyWalletAddress == null || proxyWalletAddress.trim().isEmpty()) {
      return Mono.just(false);
    }

    // Check valid Ethereum address format: 0x + 40 hex characters
    boolean isValid = proxyWalletAddress.matches("^0x[0-9a-fA-F]{40}$");
    return Mono.just(isValid);
  }
}
