package com.oregonMarkets.messaging;

import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReactiveMessagingService {
    
    private final CamelReactiveStreamsService camel;
    
    public ReactiveMessagingService(@Lazy CamelReactiveStreamsService camel) {
        this.camel = camel;
    }
    
    public Flux<String> consumeWalletRequests() {
        return Flux.from(camel.fromStream("wallet-requests", String.class));
    }
    
    public Mono<Void> publishWalletRequest(String request) {
        return Mono.from(camel.toStream("wallet-creation-requests", request, String.class)).then();
    }
}
