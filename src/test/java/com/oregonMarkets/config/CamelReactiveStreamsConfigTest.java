package com.oregonMarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CamelReactiveStreamsConfigTest {

  @Test
  void testCamelReactiveStreamsConfigInstantiation() {
    CamelReactiveStreamsConfig config = new CamelReactiveStreamsConfig();

    assertNotNull(config);
  }
}
