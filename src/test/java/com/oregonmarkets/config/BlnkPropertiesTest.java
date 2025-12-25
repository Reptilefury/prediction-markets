package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BlnkPropertiesTest {

  @Test
  void properties() {
    BlnkProperties props = new BlnkProperties();
    props.setApiUrl("http://test");
    props.setLedgerId("ledger1");
    assertEquals("http://test", props.getApiUrl());
    assertEquals("ledger1", props.getLedgerId());
  }

  @Test
  void constructor() {
    BlnkProperties props = new BlnkProperties("http://test", "ledger1");
    assertNotNull(props.toString());
  }
}
