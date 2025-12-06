package com.oregonMarkets.integration.polymarket;

import com.oregonMarkets.common.exception.ExternalServiceException;
import com.oregonMarkets.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for managing Polymarket proxy wallets for users
 *
 * <p>Leverages Polymarket's ProxyWalletFactory contract on Polygon: - Address:
 * 0xaB45c5A4B0c941a2F231C04C3f49182e1A254052
 *
 * <p>Key Architecture Notes: - Wallet creation is IMPLICIT/AUTOMATIC via the `proxy()` function -
 * Wallet address is deterministic: keccak256(abi.encodePacked(userEOA)) - Wallets are cloned from
 * an implementation contract - Uses GSN (Gas Station Network) for gasless relayed calls - No events
 * are emitted by the factory
 *
 * <p>Flow: 1. User registers -> we calculate their deterministic proxy wallet address 2. Store
 * proxy wallet address in database 3. On first proxy() call (trading), wallet is auto-created by
 * Polymarket factory
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyWalletOnboardingService {

  @Value("${app.polymarket.proxy-factory-address:0xaB45c5A4B0c941a2F231C04C3f49182e1A254052}")
  private String proxyFactoryAddress;

  private final Web3j web3j;

  /**
   * Calculate the deterministic proxy wallet address for a user
   *
   * <p>Polymarket factory creates wallets at deterministic addresses using:
   * keccak256(abi.encodePacked(userEOA))
   *
   * @param userEOA the user's Ethereum address (from Magic DID token)
   * @return Mono containing the user's proxy wallet address
   */
  public Mono<String> createUserProxyWallet(String userEOA) {
    return Mono.fromCallable(
            () -> {
              log.info("Calculating proxy wallet address for user EOA: {}", userEOA);

              if (userEOA == null || userEOA.trim().isEmpty()) {
                throw new IllegalArgumentException("User EOA address cannot be null or empty");
              }

              // Normalize the address
              String normalizedEOA =
                  userEOA.toLowerCase().startsWith("0x")
                      ? userEOA.toLowerCase()
                      : "0x" + userEOA.toLowerCase();

              try {
                // Calculate deterministic proxy wallet address using Polymarket's method
                // This matches the on-chain calculation: keccak256(abi.encodePacked(userEOA))
                String proxyWalletAddress = calculateProxyWalletAddress(normalizedEOA);

                log.info(
                    "Calculated proxy wallet address {} for user EOA {} - wallet will be auto-created on first proxy() call",
                    proxyWalletAddress,
                    normalizedEOA);

                return proxyWalletAddress;

              } catch (Exception e) {
                log.error("Failed to calculate proxy wallet address for user: {}", userEOA, e);
                throw new ExternalServiceException(
                    ResponseCode.EXTERNAL_SERVICE_ERROR,
                    "Polymarket Proxy Wallet Factory",
                    "Failed to calculate proxy wallet address: " + e.getMessage(),
                    e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> log.error("Proxy wallet calculation failed", error));
  }

  /**
   * Calculate the deterministic proxy wallet address using Polymarket's algorithm
   *
   * <p>Implements: keccak256(abi.encodePacked(userEOA)) This is the address where Polymarket's
   * factory will deploy a new proxy wallet clone
   *
   * @param userEOA normalized user Ethereum address (must start with 0x)
   * @return deterministic proxy wallet address
   */
  private String calculateProxyWalletAddress(String userEOA) {
    try {
      // Remove 0x prefix for encoding
      String addressWithoutPrefix = userEOA.substring(2);

      // abi.encodePacked(address) in Solidity is just the 20 bytes of the address
      byte[] addressBytes = Numeric.hexStringToByteArray("0x" + addressWithoutPrefix);

      // keccak256 hash of the address bytes
      byte[] hash = Hash.sha3(addressBytes);

      // Take last 20 bytes to get address
      // In Solidity CREATE2, we'd use the full hash, but Polymarket uses a simplified approach
      byte[] walletAddressBytes = new byte[20];
      System.arraycopy(hash, hash.length - 20, walletAddressBytes, 0, 20);

      // Convert to hex and format as address
      String proxyAddress =
          "0x"
              + Numeric.toHexStringNoPrefixZeroPadded(
                  new java.math.BigInteger(1, walletAddressBytes), 40);

      log.debug(
          "Calculated deterministic proxy wallet address: {} for user: {}", proxyAddress, userEOA);
      return proxyAddress;

    } catch (Exception e) {
      log.error("Failed to calculate deterministic proxy wallet address", e);
      throw new RuntimeException("Address calculation failed", e);
    }
  }

  /**
   * Get information about when user's proxy wallet will be created
   *
   * @return information message
   */
  public String getProxyWalletCreationInfo() {
    return "Proxy wallet will be automatically created by Polymarket's factory on the user's first proxy() call. "
        + "The address is deterministic and calculated now for storage in the database.";
  }

  /**
   * Verify the proxy wallet address is valid
   *
   * @param proxyWalletAddress the proxy wallet address to verify
   * @return Mono<Boolean> true if address is valid Ethereum format
   */
  public Mono<Boolean> isValidProxyWalletAddress(String proxyWalletAddress) {
    return Mono.fromCallable(
            () -> {
              if (proxyWalletAddress == null || proxyWalletAddress.trim().isEmpty()) {
                return false;
              }

              // Check valid Ethereum address format: 0x + 40 hex characters
              return proxyWalletAddress.matches("^0x[0-9a-fA-F]{40}$");
            })
        .subscribeOn(Schedulers.boundedElastic());
  }
}
