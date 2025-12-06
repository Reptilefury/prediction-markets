package com.oregonMarkets.domain.user.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoTests {

  @Test
  void userRegistrationRequest() {
    UserRegistrationRequest req = new UserRegistrationRequest();
    req.setEmail("test@test.com");
    req.setCountryCode("US");
    req.setReferralCode("ref");
    req.setUtmSource("src");
    req.setUtmMedium("med");
    req.setUtmCampaign("camp");
    assertEquals("test@test.com", req.getEmail());
  }

  @Test
  void web3RegistrationRequest() {
    Web3RegistrationRequest req = new Web3RegistrationRequest();
    req.setWalletAddress("0x742d35Cc6634C0532925a3b8D400E4C0532925a3");
    req.setSignature("sig");
    req.setMessage("msg");
    req.setCountryCode("US");
    req.setReferralCode("ref");
    assertNotNull(req.getWalletAddress());
  }

  @Test
  void userRegistrationResponse() {
    UserRegistrationResponse res =
        UserRegistrationResponse.builder()
            .userId(UUID.randomUUID())
            .email("test@test.com")
            .username("user")
            .magicWalletAddress("0x123")
            .enclaveUdaAddress("uda")
            .proxyWalletAddress("0xproxy")
            .referralCode("ref")
            .accessToken("token")
            .refreshToken("refresh")
            .createdAt(Instant.now())
            .avatarUrl("http://avatar")
            .build();
    assertNotNull(res.getUserId());
  }

  @Test
  void depositAddresses() {
    List<UserRegistrationResponse.EVMDepositAddress> evmList = new ArrayList<>();
    evmList.add(
        UserRegistrationResponse.EVMDepositAddress.builder()
            .chainId(1)
            .contractAddress("0x123")
            .deployed(true)
            .id("id1")
            .build());

    UserRegistrationResponse.DepositAddresses addresses =
        UserRegistrationResponse.DepositAddresses.builder()
            .evmDepositAddress(evmList)
            .solanaDepositAddress(
                UserRegistrationResponse.SolanaDepositAddress.builder()
                    .address("sol")
                    .id("id2")
                    .build())
            .bitcoinDepositAddress(
                UserRegistrationResponse.BitcoinDepositAddress.builder()
                    .legacyAddress("btc1")
                    .segwitAddress("btc2")
                    .nativeSegwitAddress("btc3")
                    .taprootAddress("btc4")
                    .id("id3")
                    .build())
            .build();
    assertNotNull(addresses.getEvmDepositAddress());
    assertNotNull(addresses.getSolanaDepositAddress());
    assertNotNull(addresses.getBitcoinDepositAddress());
  }

  @Test
  void depositAddresses_DefensiveCopy() {
    UserRegistrationResponse.DepositAddresses addresses =
        new UserRegistrationResponse.DepositAddresses();
    List<UserRegistrationResponse.EVMDepositAddress> list = new ArrayList<>();
    addresses.setEvmDepositAddress(list);
    list.add(UserRegistrationResponse.EVMDepositAddress.builder().build());
    assertEquals(0, addresses.getEvmDepositAddress().size());
  }
}
