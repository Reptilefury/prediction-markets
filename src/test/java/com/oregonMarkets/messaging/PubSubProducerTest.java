package com.oregonMarkets.messaging;

import static org.mockito.Mockito.*;

import com.oregonMarkets.config.GcpPubSubProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PubSubProducerTest {

  @Mock private GcpPubSubProperties props;

  @Test
  void configure() throws Exception {
    when(props.getProjectId()).thenReturn("project1");
    when(props.getTopicUniversalDepositWallet()).thenReturn("topic1");
    PubSubProducer producer = new PubSubProducer(props);
    producer.configure();
    verify(props).getProjectId();
  }
}
