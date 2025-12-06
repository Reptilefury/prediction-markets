package com.oregonMarkets.service;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class QRCodeGenerationServiceTest {

  private QRCodeGenerationService qrCodeGenerationService;

  @BeforeEach
  void setUp() {
    qrCodeGenerationService = new QRCodeGenerationService();
    ReflectionTestUtils.setField(qrCodeGenerationService, "gcpProjectId", "test-project");
    ReflectionTestUtils.setField(qrCodeGenerationService, "bucketName", "test-bucket");
    ReflectionTestUtils.setField(qrCodeGenerationService, "logoDevApiKey", "test-api-key");
  }

  @Test
  void generateAndUploadQRCodes_WithProxyWallet_Success() {
    UUID userId = UUID.randomUUID();
    String proxyWalletAddress = "0x123456789";

    StepVerifier.create(
            qrCodeGenerationService.generateAndUploadQRCodes(
                userId, proxyWalletAddress, null, null, null, null))
        .expectNextMatches(
            result ->
                result.containsKey("proxyWalletQrCode")
                    && result.get("proxyWalletQrCode").contains("test-bucket"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithEnclaveUda_Success() {
    UUID userId = UUID.randomUUID();
    String enclaveUdaAddress = "0xuda123";

    StepVerifier.create(
            qrCodeGenerationService.generateAndUploadQRCodes(
                userId, null, enclaveUdaAddress, null, null, null))
        .expectNextMatches(
            result ->
                result.containsKey("enclaveUdaQrCode")
                    && result.get("enclaveUdaQrCode").contains("test-bucket"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithEvmAddresses_Success() {
    UUID userId = UUID.randomUUID();
    Map<String, String> evmAddresses = Map.of("ethereum", "0xeth123");

    StepVerifier.create(
            qrCodeGenerationService.generateAndUploadQRCodes(
                userId, null, null, evmAddresses, null, null))
        .expectNextMatches(result -> result.containsKey("evmDepositQrCodes"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithSolanaAddress_Success() {
    UUID userId = UUID.randomUUID();
    String solanaAddress = "5gS9G9sFLNukhk8DE5deyoZNgphquLbXfdZHntGY2gjH";

    StepVerifier.create(
            qrCodeGenerationService.generateAndUploadQRCodes(
                userId, null, null, null, solanaAddress, null))
        .expectNextMatches(
            result ->
                result.containsKey("solanaDepositQrCode")
                    && result.get("solanaDepositQrCode").contains("test-bucket"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithBitcoinAddresses_Success() {
    UUID userId = UUID.randomUUID();
    Map<String, String> bitcoinAddresses = Map.of("bitcoin", "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa");

    StepVerifier.create(
            qrCodeGenerationService.generateAndUploadQRCodes(
                userId, null, null, null, null, bitcoinAddresses))
        .expectNextMatches(result -> result.containsKey("bitcoinDepositQrCodes"))
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_EmptyInputs_ReturnsEmptyMap() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(
            qrCodeGenerationService.generateAndUploadQRCodes(userId, null, null, null, null, null))
        .expectNextMatches(Map::isEmpty)
        .verifyComplete();
  }
}
