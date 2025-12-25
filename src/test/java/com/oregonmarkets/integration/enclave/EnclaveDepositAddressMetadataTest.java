package com.oregonmarkets.integration.enclave;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnclaveDepositAddressMetadataTest {

  @Test
  void metadataProvidesHelpers() {
    EnclaveDepositAddressMetadata metadata =
      EnclaveDepositAddressMetadata.builder()
        .rawAddresses(Map.of("ethereum", "0x1234", "solana", "So1anaAddr"))
        .build();

    assertTrue(metadata.hasAnyAddresses());
    assertEquals("0x1234", metadata.getDepositAddressForChain("ETHEREUM"));
    assertTrue(metadata.hasChainAddress("solana"));
    Set<String> chains = metadata.getAvailableChains();
    assertEquals(2, chains.size());
  }

  @Test
  void metadataHandlesMissingValues() {
    EnclaveDepositAddressMetadata metadata = new EnclaveDepositAddressMetadata();

    assertNull(metadata.getDepositAddressForChain("ethereum"));
    assertFalse(metadata.hasChainAddress("polygon"));
    assertTrue(metadata.getAvailableChains().isEmpty());
    assertFalse(metadata.hasAnyAddresses());
  }
}
