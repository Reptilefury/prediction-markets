package com.oregonmarkets.predictionmarkets.messaging;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PubSubConsumer extends RouteBuilder {
    
    @Override
    public void configure() {
        from("google-pubsub:{{gcp.project-id}}:{{gcp.pubsub.subscriptions.universal-deposit-wallet}}")
            .to("reactive-streams:wallet-requests");
    }
}
