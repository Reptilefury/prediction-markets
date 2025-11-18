package com.oregonmarkets.predictionmarkets.messaging;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PubSubProducer extends RouteBuilder {
    
    @Override
    public void configure() {
        from("reactive-streams:wallet-creation-requests")
            .to("google-pubsub:{{gcp.project-id}}:{{gcp.pubsub.topics.universal-deposit-wallet}}");
    }
}
