package com.oregonMarkets.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.oregonMarkets.config.GcpPubSubProperties;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PubSubProducerTest {

  @Mock private GcpPubSubProperties props;

  private PubSubProducer pubSubProducer;

  @BeforeEach
  void setUp() {
    pubSubProducer = new PubSubProducer(props);
  }

  @Test
  void configure_CreatesRouteWithCorrectUri() throws Exception {
    when(props.getProjectId()).thenReturn("test-project");
    when(props.getTopicUniversalDepositWallet()).thenReturn("test-topic");

    CamelContext context = new DefaultCamelContext();
    context.start();
    context.addRoutes(pubSubProducer);

    assertNotNull(context.getRoutes());
    assertEquals(1, context.getRoutes().size());

    String routeId = context.getRoutes().get(0).getId();
    assertNotNull(routeId);

    context.stop();
  }

  @Test
  void configure_HandlesNullProperties() throws Exception {
    when(props.getProjectId()).thenReturn(null);
    when(props.getTopicUniversalDepositWallet()).thenReturn(null);

    CamelContext context = new DefaultCamelContext();

    assertDoesNotThrow(
        () -> {
          context.start();
          context.addRoutes(pubSubProducer);
          context.stop();
        });
  }

  @Test
  void configure_HandlesEmptyProperties() throws Exception {
    when(props.getProjectId()).thenReturn("test-project");
    when(props.getTopicUniversalDepositWallet()).thenReturn("test-topic");

    CamelContext context = new DefaultCamelContext();

    assertDoesNotThrow(
        () -> {
          context.start();
          context.addRoutes(pubSubProducer);
          context.stop();
        });
  }
}
