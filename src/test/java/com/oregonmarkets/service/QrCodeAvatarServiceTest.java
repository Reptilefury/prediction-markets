package com.oregonmarkets.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QrCodeAvatarServiceTest {

  private final QrCodeAvatarService service = new QrCodeAvatarService();

  @Test
  void generateAvatarUrl() {
    String url = service.generateAvatarUrl("test");
    assertTrue(url.contains("dicebear"));
  }

  @Test
  void generateAvatarUrl_Null() {
    String url = service.generateAvatarUrl(null);
    assertTrue(url.contains("default"));
  }

  @Test
  void generateWalletQrCode() {
    String qr = service.generateWalletQrCode("0x123", "ETH");
    assertNotNull(qr);
  }

  @Test
  void generateWalletQrCode_Null() {
    assertNull(service.generateWalletQrCode(null, "ETH"));
  }

  @Test
  void generateUdaQrCode() {
    assertNotNull(service.generateUdaQrCode("uda123"));
  }

  @Test
  void generateProxyWalletQrCode() {
    assertNotNull(service.generateProxyWalletQrCode("0xproxy"));
  }

  @Test
  void generateSolanaDepositQrCode() {
    String json = "{\"solana_deposit_address\":{\"address\":\"sol123\"}}";
    assertNotNull(service.generateSolanaDepositQrCode(json));
  }

  @Test
  void generateSolanaDepositQrCode_Invalid() {
    assertNull(service.generateSolanaDepositQrCode("invalid"));
  }

  @Test
  void generateBitcoinDepositQrCodes() {
    String json =
        "{\"bitcoin_deposit_address\":{\"legacy_address\":\"btc1\",\"segwit_address\":\"btc2\"}}";
    assertNotNull(service.generateBitcoinDepositQrCodes(json));
  }

  @Test
  void generateDepositQrCodesJson() {
    assertNotNull(service.generateDepositQrCodesJson("test"));
  }
}
