package com.oregonMarkets.messaging;

import org.apache.camel.CamelContext;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReactiveMessagingService {

  private final CamelContext camelContext;
  private volatile CamelReactiveStreamsService camel;

  public ReactiveMessagingService(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

  private CamelReactiveStreamsService camel() {
    CamelReactiveStreamsService local = this.camel;
    if (local == null) {
      synchronized (this) {
        if (this.camel == null) {
          this.camel = CamelReactiveStreams.get(camelContext);
        }
        local = this.camel;
      }
    }
    return local;
  }

  public Flux<String> consumeWalletRequests() {
    return Flux.from(camel().fromStream("wallet-requests", String.class));
  }

  public Mono<Void> publishWalletRequest(String request) {
    return Mono.from(camel().toStream("wallet-creation-requests", request, String.class)).then();
  }
}
