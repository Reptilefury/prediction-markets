package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GcpPubSubPropertiesTest {

  @Test
  void properties() {
    GcpPubSubProperties props = new GcpPubSubProperties("project1", "topic1", "sub1");
    assertEquals("project1", props.getProjectId());
    assertEquals("topic1", props.getTopicUniversalDepositWallet());
    assertEquals("sub1", props.getSubscriptionUniversalDepositWallet());
  }

  @Test
  void properties_WithWhitespace() {
    GcpPubSubProperties props = new GcpPubSubProperties(" project1 ", " topic1 ", " sub1 ");
    assertEquals("project1", props.getProjectId());
  }

  @Test
  void properties_Null() {
    GcpPubSubProperties props = new GcpPubSubProperties(null, null, null);
    assertNull(props.getProjectId());
  }
}
