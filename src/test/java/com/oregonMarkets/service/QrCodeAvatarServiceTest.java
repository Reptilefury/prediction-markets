package com.oregonMarkets.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeAvatarServiceTest {

    private QrCodeAvatarService service;

    @BeforeEach
    void setUp() {
        service = new QrCodeAvatarService();
    }

    @Test
    void generateAvatarUrl_WithSeed() {
        String result = service.generateAvatarUrl("test-seed");
        
        assertTrue(result.contains("api.dicebear.com"));
        assertTrue(result.contains("test-seed"));
    }

    @Test
    void generateAvatarUrl_WithNullSeed() {
        String result = service.generateAvatarUrl(null);
        
        assertTrue(result.contains("api.dicebear.com"));
        assertTrue(result.contains("default"));
    }

    @Test
    void generateWalletQrCode_Success() {
        String result = service.generateWalletQrCode("0x123", "ETH");
        
        assertTrue(result.contains("api.qrserver.com"));
        assertTrue(result.contains("ETH%3A0x123"));
    }

    @Test
    void generateWalletQrCode_NullAddress() {
        String result = service.generateWalletQrCode(null, "ETH");
        
        assertNull(result);
    }

    @Test
    void generateUdaQrCode() {
        String result = service.generateUdaQrCode("uda-address");
        
        assertTrue(result.contains("UDA%3Auda-address"));
    }

    @Test
    void generateProxyWalletQrCode() {
        String result = service.generateProxyWalletQrCode("proxy-address");
        
        assertTrue(result.contains("Proxy%3Aproxy-address"));
    }

    @Test
    void generateSolanaDepositQrCode_ValidJson() {
        String json = "{\"solana_deposit_address\":{\"address\":\"solana123\"}}";
        
        String result = service.generateSolanaDepositQrCode(json);
        
        assertTrue(result.contains("Solana%3Asolana123"));
    }

    @Test
    void generateSolanaDepositQrCode_InvalidJson() {
        String result = service.generateSolanaDepositQrCode("invalid-json");
        
        assertNull(result);
    }

    @Test
    void generateBitcoinDepositQrCodes_ValidJson() {
        String json = "{\"bitcoin_deposit_address\":{\"legacy_address\":\"btc123\"}}";
        
        String result = service.generateBitcoinDepositQrCodes(json);
        
        assertTrue(result.contains("legacy"));
        assertTrue(result.contains("btc123"));
    }

    @Test
    void generateDepositQrCodesJson_ValidInput() {
        String result = service.generateDepositQrCodesJson(Map.of("test", "value"));
        
        assertTrue(result.contains("generated_at"));
        assertTrue(result.contains("qr_codes"));
    }

    @Test
    void generateDepositQrCodesJson_NullInput() {
        String result = service.generateDepositQrCodesJson(null);
        
        assertNull(result);
    }
}