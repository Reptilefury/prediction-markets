package com.oregonmarkets.integration.magic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MagicDIDValidatorSimpleTest {

  @Test
  void magicUserInfo_Constructor() {
    MagicDIDValidator.MagicUserInfo info =
        new MagicDIDValidator.MagicUserInfo("iss", "email", "addr", "phone", "uid");
    assertEquals("iss", info.getIssuer());
    assertEquals("email", info.getEmail());
    assertEquals("addr", info.getPublicAddress());
    assertEquals("phone", info.getPhone());
    assertEquals("uid", info.getUserId());
  }

  @Test
  void magicUserInfo_Setters() {
    MagicDIDValidator.MagicUserInfo info = new MagicDIDValidator.MagicUserInfo();
    info.setIssuer("i");
    info.setEmail("e");
    info.setPublicAddress("a");
    info.setPhone("p");
    info.setUserId("u");
    assertNotNull(info.toString());
  }
}
