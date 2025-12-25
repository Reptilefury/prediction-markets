package com.oregonmarkets.domain.user.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void builder() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .email("test@test.com")
            .username("testuser")
            .displayName("Test User")
            .magicUserId("magic123")
            .magicWalletAddress("0x123")
            .magicIssuer("did:ethr:0x123")
            .web3WalletAddress("0x456")
            .authMethod(User.AuthMethod.MAGIC)
            .enclaveUserId("enclave123")
            .enclaveUdaAddress("uda123")
            .enclaveUdaStatus(User.EnclaveUdaStatus.ACTIVE)
            .countryCode("US")
            .isActive(true)
            .emailVerified(true)
            .kycStatus(User.KycStatus.APPROVED)
            .kycLevel(1)
            .dailyDepositLimit(BigDecimal.valueOf(10000))
            .dailyWithdrawalLimit(BigDecimal.valueOf(5000))
            .blnkIdentityId("blnk123")
            .blnkBalanceId("bal123")
            .proxyWalletAddress("0xproxy")
            .proxyWalletStatus(User.ProxyWalletStatus.ACTIVE)
            .referralCode("REF123")
            .avatarUrl("http://avatar")
            .build();
    assertNotNull(user);
    assertEquals("test@test.com", user.getEmail());
  }

  @Test
  void enums() {
    assertEquals(4, User.EnclaveUdaStatus.values().length);
    assertEquals(4, User.KycStatus.values().length);
    assertEquals(2, User.AuthMethod.values().length);
    assertEquals(4, User.ProxyWalletStatus.values().length);
  }

  @Test
  void setters() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("test@test.com");
    user.setUsername("user");
    user.setMagicUserId("magic");
    user.setEnclaveUdaAddress("uda");
    user.setProxyWalletAddress("0xproxy");
    user.setBlnkIdentityId("blnk");
    user.setReferralCode("REF");
    user.setAvatarUrl("http://avatar");
    assertNotNull(user.getId());
  }
}
