package com.oregonMarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;

class Web3jConfigTest {

  @Test
  void web3j_CreatesInstance() {
    Web3jConfig config = new Web3jConfig();
    Web3j web3j = config.web3j();

    assertNotNull(web3j);
  }
}
