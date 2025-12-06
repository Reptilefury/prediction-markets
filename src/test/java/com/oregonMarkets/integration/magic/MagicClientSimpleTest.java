package com.oregonMarkets.integration.magic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MagicClientSimpleTest {

  @Test
  void magicUserInfo_AllFields() {
    MagicClient.MagicUserInfo info = new MagicClient.MagicUserInfo();
    info.setIssuer("iss");
    info.setEmail("e@e.com");
    info.setPublicAddress("0x1");
    assertEquals("iss", info.getIssuer());
    assertEquals("e@e.com", info.getEmail());
    assertEquals("0x1", info.getPublicAddress());
    assertNotNull(info.toString());
  }
}
