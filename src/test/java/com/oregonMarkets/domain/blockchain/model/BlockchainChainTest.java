package com.oregonMarkets.domain.blockchain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockchainChainTest {

  @Test
  void builder() {
    BlockchainChain chain =
        BlockchainChain.builder()
            .id(UUID.randomUUID())
            .chainName("polygon")
            .chainId(137)
            .rpcUrl("http://test")
            .usdcTokenAddress("0x123")
            .usdcDecimals(6)
            .requiredConfirmations(12)
            .isActive(true)
            .lastScannedBlock(100L)
            .build();
    assertNotNull(chain);
    assertEquals("polygon", chain.getChainName());
  }

  @Test
  void setters() {
    BlockchainChain chain = new BlockchainChain();
    chain.setId(UUID.randomUUID());
    chain.setChainName("ethereum");
    chain.setChainId(1);
    chain.setRpcUrl("http://eth");
    chain.setUsdcTokenAddress("0x456");
    chain.setUsdcDecimals(6);
    chain.setRequiredConfirmations(12);
    chain.setIsActive(true);
    chain.setLastScannedBlock(200L);
    chain.setCreatedAt(Instant.now());
    assertEquals("ethereum", chain.getChainName());
  }
}
