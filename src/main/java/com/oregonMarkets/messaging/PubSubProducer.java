package com.oregonmarkets.messaging;

import com.oregonmarkets.config.GcpPubSubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PubSubProducer extends RouteBuilder {

  private final GcpPubSubProperties props;

  @Override
  public void configure() {
    String topicUri =
        String.format(
            "google-pubsub:%s:%s?lazyStartProducer=true",
            props.getProjectId(), props.getTopicUniversalDepositWallet());
    log.info("PubSubProducer topic URI: {}", topicUri);
    from("reactive-streams:wallet-creation-requests").to(topicUri);
  }
}
