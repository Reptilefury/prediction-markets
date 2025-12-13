package com.oregonMarkets.domain.enclave.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnclaveChainAddressTest {

  @Test
  void builder_CreatesEnclaveChainAddressWithAllFields() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    EnclaveChainAddress address =
        EnclaveChainAddress.builder()
            .id(id)
            .userId(userId)
            .chainType(EnclaveChainAddress.ChainType.ETHEREUM)
            .chainId(1)
            .depositAddress("0x123456789")
            .isActive(true)
            .build();

    assertEquals(id, address.getId());
    assertEquals(userId, address.getUserId());
    assertEquals(EnclaveChainAddress.ChainType.ETHEREUM, address.getChainType());
    assertEquals(1, address.getChainId());
    assertEquals("0x123456789", address.getDepositAddress());
    assertTrue(address.getIsActive());
    // createdAt removed for security - no longer tested
  }

  @Test
  void chainType_EnumValues() {
    assertEquals("ETHEREUM", EnclaveChainAddress.ChainType.ETHEREUM.name());
    assertEquals("POLYGON", EnclaveChainAddress.ChainType.POLYGON.name());
    assertEquals("ARBITRUM", EnclaveChainAddress.ChainType.ARBITRUM.name());
    assertEquals("OPTIMISM", EnclaveChainAddress.ChainType.OPTIMISM.name());
    assertEquals("BASE", EnclaveChainAddress.ChainType.BASE.name());
  }

  @Test
  void chainType_EnumValuesExist() {
    EnclaveChainAddress.ChainType[] values = EnclaveChainAddress.ChainType.values();
    assertTrue(values.length >= 5);

    boolean hasEthereum = false;
    boolean hasPolygon = false;
    boolean hasArbitrum = false;
    boolean hasOptimism = false;
    boolean hasBase = false;

    for (EnclaveChainAddress.ChainType type : values) {
      switch (type) {
        case ETHEREUM -> hasEthereum = true;
        case POLYGON -> hasPolygon = true;
        case ARBITRUM -> hasArbitrum = true;
        case OPTIMISM -> hasOptimism = true;
        case BASE -> hasBase = true;
      }
    }

    assertTrue(hasEthereum);
    assertTrue(hasPolygon);
    assertTrue(hasArbitrum);
    assertTrue(hasOptimism);
    assertTrue(hasBase);
  }
}
