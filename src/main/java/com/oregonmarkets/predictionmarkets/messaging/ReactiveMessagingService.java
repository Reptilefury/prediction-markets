package com.oregonmarkets.predictionmarkets.messaging;

import org.apache.camel.CamelContext;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReactiveMessagingService {
    
    @Autowired
    private CamelReactiveStreamsService camel;
    
    public Flux<String> consumeMarketEvents() {
        return camel.fromStream("market-events", String.class);
    }
    
    public Mono<Void> publishEvent(String event) {
        return camel.toStream("outgoing-events", event, String.class).then();
    }
}
