package com.oregonMarkets.domain.user.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RequestDtoSimpleTest {

  @Test
  void userRegistrationRequest() {
    UserRegistrationRequest req = new UserRegistrationRequest();
    req.setEmail("a@a.com");
    req.setCountryCode("US");
    req.setReferralCode("R");
    req.setUtmSource("s");
    req.setUtmMedium("m");
    req.setUtmCampaign("c");
    assertEquals("a@a.com", req.getEmail());
    assertEquals("US", req.getCountryCode());
    assertEquals("R", req.getReferralCode());
    assertEquals("s", req.getUtmSource());
    assertEquals("m", req.getUtmMedium());
    assertEquals("c", req.getUtmCampaign());
  }

  @Test
  void web3RegistrationRequest() {
    Web3RegistrationRequest req = new Web3RegistrationRequest();
    req.setWalletAddress("0x1");
    req.setSignature("sig");
    req.setMessage("msg");
    req.setCountryCode("US");
    req.setReferralCode("R");
    req.setUtmSource("s");
    req.setUtmMedium("m");
    req.setUtmCampaign("c");
    assertEquals("0x1", req.getWalletAddress());
    assertEquals("sig", req.getSignature());
    assertEquals("msg", req.getMessage());
  }
}
