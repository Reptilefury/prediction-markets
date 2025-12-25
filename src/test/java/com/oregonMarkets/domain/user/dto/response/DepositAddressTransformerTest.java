package com.oregonmarkets.domain.user.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DepositAddressTransformerTest {

  @Test
  void transform_NullInput_ReturnsNull() {
    UserRegistrationResponse.DepositAddresses result = DepositAddressTransformer.transform(null);
    assertNull(result);
  }

  @Test
  void transform_CompleteDepositAddresses_TransformsCorrectly() {
    Map<String, Object> rawDepositAddresses =
        Map.of(
            "evm_deposit_address",
                List.of(
                    Map.of(
                        "chainId",
                        1,
                        "contractAddress",
                        "0x123",
                        "deployed",
                        true,
                        "_id",
                        "evm-id-1"),
                    Map.of(
                        "chainId",
                        137,
                        "contractAddress",
                        "0x456",
                        "deployed",
                        false,
                        "_id",
                        "evm-id-2")),
            "solana_deposit_address",
                Map.of(
                    "address", "solana-address-123",
                    "_id", "solana-id-1"),
            "bitcoin_deposit_address",
                Map.of(
                    "legacy_address", "legacy-btc-address",
                    "segwit_address", "segwit-btc-address",
                    "native_segwit_address", "native-segwit-address",
                    "taproot_address", "taproot-address",
                    "_id", "btc-id-1"));

    UserRegistrationResponse.DepositAddresses result =
        DepositAddressTransformer.transform(rawDepositAddresses);

    assertNotNull(result);

    // Verify EVM addresses
    assertNotNull(result.getEvmDepositAddress());
    assertEquals(2, result.getEvmDepositAddress().size());

    UserRegistrationResponse.EVMDepositAddress evmAddr1 = result.getEvmDepositAddress().get(0);
    assertEquals(1, evmAddr1.getChainId());
    assertEquals("0x123", evmAddr1.getContractAddress());
    assertTrue(evmAddr1.getDeployed());
    assertEquals("evm-id-1", evmAddr1.getId());

    UserRegistrationResponse.EVMDepositAddress evmAddr2 = result.getEvmDepositAddress().get(1);
    assertEquals(137, evmAddr2.getChainId());
    assertEquals("0x456", evmAddr2.getContractAddress());
    assertFalse(evmAddr2.getDeployed());
    assertEquals("evm-id-2", evmAddr2.getId());

    // Verify Solana address
    assertNotNull(result.getSolanaDepositAddress());
    assertEquals("solana-address-123", result.getSolanaDepositAddress().getAddress());
    assertEquals("solana-id-1", result.getSolanaDepositAddress().getId());

    // Verify Bitcoin address
    assertNotNull(result.getBitcoinDepositAddress());
    assertEquals("legacy-btc-address", result.getBitcoinDepositAddress().getLegacyAddress());
    assertEquals("segwit-btc-address", result.getBitcoinDepositAddress().getSegwitAddress());
    assertEquals(
        "native-segwit-address", result.getBitcoinDepositAddress().getNativeSegwitAddress());
    assertEquals("taproot-address", result.getBitcoinDepositAddress().getTaprootAddress());
    assertEquals("btc-id-1", result.getBitcoinDepositAddress().getId());
  }

  @Test
  void transform_NullEvmAddresses_HandlesGracefully() {
    Map<String, Object> rawDepositAddresses =
        Map.of(
            "solana_deposit_address",
            Map.of(
                "address", "solana-address-123",
                "_id", "solana-id-1"));

    UserRegistrationResponse.DepositAddresses result =
        DepositAddressTransformer.transform(rawDepositAddresses);

    assertNotNull(result);
    // EVM addresses can be null or empty list depending on implementation
    assertNotNull(result.getSolanaDepositAddress());
  }

  @Test
  void transform_NullSolanaAddress_HandlesGracefully() {
    Map<String, Object> rawDepositAddresses =
        Map.of(
            "evm_deposit_address",
            List.of(
                Map.of(
                    "chainId",
                    1,
                    "contractAddress",
                    "0x123",
                    "deployed",
                    true,
                    "_id",
                    "evm-id-1")));

    UserRegistrationResponse.DepositAddresses result =
        DepositAddressTransformer.transform(rawDepositAddresses);

    assertNotNull(result);
    assertNotNull(result.getEvmDepositAddress());
    assertNull(result.getSolanaDepositAddress());
    assertNull(result.getBitcoinDepositAddress());
  }

  @Test
  void transform_NullBitcoinAddress_HandlesGracefully() {
    Map<String, Object> rawDepositAddresses =
        Map.of(
            "evm_deposit_address",
                List.of(
                    Map.of(
                        "chainId",
                        1,
                        "contractAddress",
                        "0x123",
                        "deployed",
                        true,
                        "_id",
                        "evm-id-1")),
            "solana_deposit_address",
                Map.of(
                    "address", "solana-address-123",
                    "_id", "solana-id-1"));

    UserRegistrationResponse.DepositAddresses result =
        DepositAddressTransformer.transform(rawDepositAddresses);

    assertNotNull(result);
    assertNotNull(result.getEvmDepositAddress());
    assertNotNull(result.getSolanaDepositAddress());
    assertNull(result.getBitcoinDepositAddress());
  }

  @Test
  void transform_EmptyEvmAddressList_HandlesGracefully() {
    Map<String, Object> rawDepositAddresses = Map.of("evm_deposit_address", List.of());

    UserRegistrationResponse.DepositAddresses result =
        DepositAddressTransformer.transform(rawDepositAddresses);

    assertNotNull(result);
    assertNotNull(result.getEvmDepositAddress());
    assertTrue(result.getEvmDepositAddress().isEmpty());
  }
}
