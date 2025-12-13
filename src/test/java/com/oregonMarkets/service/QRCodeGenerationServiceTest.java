package com.oregonMarkets.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

class QRCodeGenerationServiceTest {

  private Storage mockStorage;
  private QRCodeGenerationService service;

  @BeforeEach
  void setUp() {
    mockStorage = mock(Storage.class);
    service = new QRCodeGenerationService(mockStorage);

    // Set required fields for QR code generation
    ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
    ReflectionTestUtils.setField(service, "gcpProjectId", "test-project");
    ReflectionTestUtils.setField(service, "logoDevPublishableKey", "test-key");

    // Mock successful storage uploads by default
    Blob mockBlob = mock(Blob.class);
    when(mockStorage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(mockBlob);
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

  @Test
  void generateAndUploadQRCodes_UploadSuccess_VerifiesStorageCalled() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(service.generateAndUploadQRCodes(userId, "0xproxy", null, null, null, null))
        .expectNextMatches(map -> {
          // Verify that storage.create was called
          verify(mockStorage, atLeastOnce()).create(any(BlobInfo.class), any(byte[].class));
          return map.containsKey("proxyWalletQrCode");
        })
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_UploadFailure_ReturnsMockUrl() {
    // Simulate storage failure
    when(mockStorage.create(any(BlobInfo.class), any(byte[].class)))
        .thenThrow(new StorageException(500, "Storage error"));

    UUID userId = UUID.randomUUID();

    StepVerifier.create(service.generateAndUploadQRCodes(userId, "0xproxy", null, null, null, null))
        .expectNextMatches(map -> {
          // Should still return a URL (mock URL) even on failure
          String url = map.get("proxyWalletQrCode");
          return url != null && url.contains("storage.googleapis.com");
        })
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_MultipleAddresses_UploadsAll() {
    UUID userId = UUID.randomUUID();
    Map<String, String> evmAddresses = Map.of("ethereum", "0xeth", "polygon", "0xpoly");

    StepVerifier.create(
            service.generateAndUploadQRCodes(
                userId, "0xproxy", "0xenclave", evmAddresses, "solana123", null))
        .expectNextMatches(map -> {
          // Verify multiple uploads happened
          verify(mockStorage, atLeast(4)).create(any(BlobInfo.class), any(byte[].class));
          return map.size() >= 4;
        })
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_WithNullAddresses_HandlesGracefully() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(
            service.generateAndUploadQRCodes(userId, null, null, null, null, null))
        .expectNextMatches(Map::isEmpty)
        .verifyComplete();
  }

  @Test
  void generateAndUploadQRCodes_VerifiesBlobInfoCreation() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(service.generateAndUploadQRCodes(userId, "0xproxy", null, null, null, null))
        .expectNextMatches(map -> {
          // Verify BlobInfo was created with correct content type
          verify(mockStorage).create(
              argThat(blobInfo ->
                  blobInfo.getContentType().equals("image/png") &&
                  blobInfo.getBucket().equals("test-bucket") &&
                  blobInfo.getName().contains(userId.toString())
              ),
              any(byte[].class)
          );
          return true;
        })
        .verifyComplete();
  }
}

