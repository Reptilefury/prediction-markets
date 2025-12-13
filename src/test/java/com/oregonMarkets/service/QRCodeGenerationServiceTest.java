package com.oregonMarkets.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.cloud.storage.Storage;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class QRCodeGenerationServiceTest {

  private Storage mockStorage;
  private QRCodeGenerationService service;

  @BeforeEach
  void setUp() {
    mockStorage = mock(Storage.class);
    service = new QRCodeGenerationService(mockStorage);
  }

  @Test
  void generateAndUploadQRCodes_WithProxyWallet() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(service.generateAndUploadQRCodes(userId, "0xproxy", null, null, null, null))
        .expectNextMatches(map -> map.containsKey("proxyWalletQrCode"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithEnclaveUda() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(
            service.generateAndUploadQRCodes(userId, null, "0xenclave", null, null, null))
        .expectNextMatches(map -> map.containsKey("enclaveUdaQrCode"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithEvmAddresses() {
    UUID userId = UUID.randomUUID();
    Map<String, String> evmAddresses = Map.of("ethereum", "0xeth", "polygon", "0xpoly");

    StepVerifier.create(
            service.generateAndUploadQRCodes(userId, null, null, evmAddresses, null, null))
        .expectNextMatches(map -> map.containsKey("evmDepositQrCodes"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithSolana() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(
            service.generateAndUploadQRCodes(userId, null, null, null, "solana123", null))
        .expectNextMatches(map -> map.containsKey("solanaDepositQrCode"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithBitcoin() {
    UUID userId = UUID.randomUUID();
    Map<String, String> btcAddresses = Map.of("bitcoin", "bc1btc");

    StepVerifier.create(
            service.generateAndUploadQRCodes(userId, null, null, null, null, btcAddresses))
        .expectNextMatches(map -> map.containsKey("bitcoinDepositQrCodes"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_AllAddresses() {
    UUID userId = UUID.randomUUID();
    Map<String, String> evmAddresses = Map.of("ethereum", "0xeth");
    Map<String, String> btcAddresses = Map.of("bitcoin", "bc1btc");

    StepVerifier.create(
            service.generateAndUploadQRCodes(
                userId, "0xproxy", "0xenclave", evmAddresses, "solana123", btcAddresses))
        .expectNextMatches(map -> map.size() >= 4)
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_EmptyAddresses() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(service.generateAndUploadQRCodes(userId, "", "", Map.of(), "", Map.of()))
        .expectNextMatches(Map::isEmpty)
        .verifyComplete();
  }
}
