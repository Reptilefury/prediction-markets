package com.oregonMarkets.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QrCodeAvatarServiceFullTest {

  private final QrCodeAvatarService service = new QrCodeAvatarService();

  @Test
  void generateAvatarUrl_WithSeed() {
    assertNotNull(service.generateAvatarUrl("test"));
  }

  @Test
  void generateAvatarUrl_NullSeed() {
    assertNotNull(service.generateAvatarUrl(null));
  }

  @Test
  void generateAvatarUrl_EmptySeed() {
    assertNotNull(service.generateAvatarUrl(""));
  }

  @Test
  void generateWalletQrCode_Valid() {
    assertNotNull(service.generateWalletQrCode("addr", "label"));
  }

  @Test
  void generateWalletQrCode_NullAddress() {
    assertNull(service.generateWalletQrCode(null, "label"));
  }

  @Test
  void generateWalletQrCode_EmptyAddress() {
    assertNull(service.generateWalletQrCode("", "label"));
  }

  @Test
  void generateUdaQrCode_Valid() {
    assertNotNull(service.generateUdaQrCode("uda"));
  }

  @Test
  void generateUdaQrCode_Null() {
    assertNull(service.generateUdaQrCode(null));
  }

  @Test
  void generateProxyWalletQrCode_Valid() {
    assertNotNull(service.generateProxyWalletQrCode("proxy"));
  }

  @Test
  void generateProxyWalletQrCode_Null() {
    assertNull(service.generateProxyWalletQrCode(null));
  }

  @Test
  void generateSolanaDepositQrCode_Valid() {
    String json = "{\"solana_deposit_address\":{\"address\":\"sol\"}}";
    assertNotNull(service.generateSolanaDepositQrCode(json));
  }

  @Test
  void generateSolanaDepositQrCode_Null() {
    assertNull(service.generateSolanaDepositQrCode(null));
  }

  @Test
  void generateSolanaDepositQrCode_InvalidJson() {
    assertNull(service.generateSolanaDepositQrCode("invalid"));
  }

  @Test
  void generateSolanaDepositQrCode_NoAddress() {
    assertNull(service.generateSolanaDepositQrCode("{}"));
  }

  @Test
  void generateBitcoinDepositQrCodes_Valid() {
    String json = "{\"bitcoin_deposit_address\":{\"legacy_address\":\"btc\"}}";
    assertNotNull(service.generateBitcoinDepositQrCodes(json));
  }

  @Test
  void generateBitcoinDepositQrCodes_Null() {
    assertNull(service.generateBitcoinDepositQrCodes(null));
  }

  @Test
  void generateBitcoinDepositQrCodes_InvalidJson() {
    assertNull(service.generateBitcoinDepositQrCodes("invalid"));
  }

  @Test
  void generateBitcoinDepositQrCodes_AllFields() {
    String json =
        "{\"bitcoin_deposit_address\":{\"legacy_address\":\"l\",\"segwit_address\":\"s\",\"native_segwit_address\":\"n\",\"taproot_address\":\"t\"}}";
    assertNotNull(service.generateBitcoinDepositQrCodes(json));
  }

  @Test
  void generateDepositQrCodesJson_Valid() {
    assertNotNull(service.generateDepositQrCodesJson("test"));
  }

  @Test
  void generateDepositQrCodesJson_Null() {
    assertNull(service.generateDepositQrCodesJson(null));
  }
}
