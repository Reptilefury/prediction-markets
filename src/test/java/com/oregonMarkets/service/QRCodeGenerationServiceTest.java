package com.oregonMarkets.service;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeGenerationServiceTest {

    @Test
    void generateAndUploadQRCodes_WithProxyWallet() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, "0xproxy", null, null, null, null))
                .expectNextMatches(map -> map.containsKey("proxyWalletQrCode"))
                .verifyComplete();
    }

    @Test
    void generateAndUploadQRCodes_WithEnclaveUda() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, null, "0xenclave", null, null, null))
                .expectNextMatches(map -> map.containsKey("enclaveUdaQrCode"))
                .verifyComplete();
    }

    @Test
    void generateAndUploadQRCodes_WithEvmAddresses() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();
        Map<String, String> evmAddresses = Map.of("ethereum", "0xeth", "polygon", "0xpoly");

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, null, null, evmAddresses, null, null))
                .expectNextMatches(map -> map.containsKey("evmDepositQrCodes"))
                .verifyComplete();
    }

    @Test
    void generateAndUploadQRCodes_WithSolana() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, null, null, null, "solana123", null))
                .expectNextMatches(map -> map.containsKey("solanaDepositQrCode"))
                .verifyComplete();
    }

    @Test
    void generateAndUploadQRCodes_WithBitcoin() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();
        Map<String, String> btcAddresses = Map.of("bitcoin", "bc1btc");

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, null, null, null, null, btcAddresses))
                .expectNextMatches(map -> map.containsKey("bitcoinDepositQrCodes"))
                .verifyComplete();
    }

    @Test
    void generateAndUploadQRCodes_AllAddresses() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();
        Map<String, String> evmAddresses = Map.of("ethereum", "0xeth");
        Map<String, String> btcAddresses = Map.of("bitcoin", "bc1btc");

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, "0xproxy", "0xenclave", evmAddresses, "solana123", btcAddresses))
                .expectNextMatches(map -> map.size() >= 4)
                .verifyComplete();
    }

    @Test
    void generateAndUploadQRCodes_EmptyAddresses() {
        QRCodeGenerationService service = new QRCodeGenerationService();
        UUID userId = UUID.randomUUID();

        StepVerifier.create(service.generateAndUploadQRCodes(
                userId, "", "", Map.of(), "", Map.of()))
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();
    }
}
