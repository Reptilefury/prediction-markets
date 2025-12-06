package com.oregonMarkets.messaging;

import org.apache.camel.CamelContext;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveMessagingServiceTest {

    @Mock
    private CamelContext camelContext;
    
    @Mock
    private CamelReactiveStreamsService camelReactiveStreamsService;

    private ReactiveMessagingService reactiveMessagingService;

    @BeforeEach
    void setUp() {
        reactiveMessagingService = new ReactiveMessagingService(camelContext);
    }

    @Test
    void consumeWalletRequests_Success() {
        try (MockedStatic<CamelReactiveStreams> mockedStatic = mockStatic(CamelReactiveStreams.class)) {
            mockedStatic.when(() -> CamelReactiveStreams.get(camelContext))
                .thenReturn(camelReactiveStreamsService);
            
            when(camelReactiveStreamsService.fromStream("wallet-requests", String.class))
                .thenReturn(Flux.just("request1", "request2"));

            StepVerifier.create(reactiveMessagingService.consumeWalletRequests())
                .expectNext("request1")
                .expectNext("request2")
                .verifyComplete();
        }
    }

    @Test
    void publishWalletRequest_Success() {
        try (MockedStatic<CamelReactiveStreams> mockedStatic = mockStatic(CamelReactiveStreams.class)) {
            mockedStatic.when(() -> CamelReactiveStreams.get(camelContext))
                .thenReturn(camelReactiveStreamsService);
            
            when(camelReactiveStreamsService.toStream("wallet-creation-requests", "test-request", String.class))
                .thenReturn(Mono.just("test-request"));

            StepVerifier.create(reactiveMessagingService.publishWalletRequest("test-request"))
                .verifyComplete();
        }
    }

    @Test
    void camel_LazyInitialization() {
        try (MockedStatic<CamelReactiveStreams> mockedStatic = mockStatic(CamelReactiveStreams.class)) {
            mockedStatic.when(() -> CamelReactiveStreams.get(camelContext))
                .thenReturn(camelReactiveStreamsService);
            
            when(camelReactiveStreamsService.fromStream("wallet-requests", String.class))
                .thenReturn(Flux.just("request1"));

            // First call should initialize
            reactiveMessagingService.consumeWalletRequests().blockFirst();
            
            // Second call should use cached instance
            reactiveMessagingService.consumeWalletRequests().blockFirst();
            
            // Verify CamelReactiveStreams.get was called only once
            mockedStatic.verify(() -> CamelReactiveStreams.get(camelContext), times(1));
        }
    }
}