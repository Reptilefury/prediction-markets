package com.oregonmarkets.integration.enclave;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.*;

/**
 * Metadata structure for Enclave deposit addresses Borrowed from the multi-chain Enclave UDA design
 *
 * <p>Enclave returns deposit addresses as Map<String, Object> containing chain-specific addresses.
 * This class provides typed access to the deposit address structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnclaveDepositAddressMetadata {

  /**
   * Raw deposit addresses map from Enclave API Structure: { "ethereum": "0x...", "polygon":
   * "0x...", "arbitrum": "0x...", etc }
   */
  private Map<String, Object> rawAddresses;

  /**
   * Extract specific chain deposit address
   *
   * @param chainType ethereum, polygon, arbitrum, optimism, base, bsc, avalanche
   * @return deposit address for the chain or null if not found
   */
  public String getDepositAddressForChain(String chainType) {
    if (rawAddresses == null || chainType == null) {
      return null;
    }
    Object address = rawAddresses.get(chainType.toLowerCase());
    return address != null ? address.toString() : null;
  }

  /** Check if deposit addresses are available for a specific chain */
  public boolean hasChainAddress(String chainType) {
    return getDepositAddressForChain(chainType) != null;
  }

  /** Get all available chains from deposit addresses */
  public java.util.Set<String> getAvailableChains() {
    return rawAddresses != null ? rawAddresses.keySet() : java.util.Collections.emptySet();
  }

  /** Check if any deposit addresses are available */
  public boolean hasAnyAddresses() {
    return rawAddresses != null && !rawAddresses.isEmpty();
  }
}
