package com.oregonMarkets.messaging;

import com.oregonMarkets.config.GcpPubSubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PubSubConsumer extends RouteBuilder {

    private final GcpPubSubProperties props;
    
    @Override
    public void configure() {
        String subscriptionUri = String.format(
            "google-pubsub:%s:%s",
            props.getProjectId(),
            props.getSubscriptionUniversalDepositWallet()
        );
        log.info("PubSubConsumer subscription URI: {}", subscriptionUri);
        from(subscriptionUri)
            .to("reactive-streams:wallet-requests");
    }
}
