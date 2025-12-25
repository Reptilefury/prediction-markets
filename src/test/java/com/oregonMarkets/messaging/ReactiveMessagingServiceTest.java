package com.oregonmarkets.messaging;

import org.apache.camel.CamelContext;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    // Given
    String testRequest = "test request";
    Flux<String> expectedFlux = Flux.just(testRequest);

    try (var mockStatic = mockStatic(CamelReactiveStreams.class)) {
      mockStatic.when(() -> CamelReactiveStreams.get(camelContext))
          .thenReturn(camelReactiveStreamsService);
      when(camelReactiveStreamsService.fromStream("wallet-requests", String.class))
          .thenReturn(expectedFlux);

      // When
      Flux<String> result = reactiveMessagingService.consumeWalletRequests();

      // Then
      StepVerifier.create(result)
          .expectNext(testRequest)
          .verifyComplete();

      verify(camelReactiveStreamsService).fromStream("wallet-requests", String.class);
    }
  }

  @Test
  void publishWalletRequest_Success() {
    // Given
    String testRequest = "test request";
    Mono<String> expectedMono = Mono.just(testRequest);

    try (var mockStatic = mockStatic(CamelReactiveStreams.class)) {
      mockStatic.when(() -> CamelReactiveStreams.get(camelContext))
          .thenReturn(camelReactiveStreamsService);
      when(camelReactiveStreamsService.toStream(
          eq("wallet-creation-requests"),
          eq(testRequest),
          eq(String.class)))
          .thenReturn(expectedMono);

      // When
      Mono<Void> result = reactiveMessagingService.publishWalletRequest(testRequest);

      // Then
      StepVerifier.create(result)
          .verifyComplete();

      verify(camelReactiveStreamsService).toStream(
          "wallet-creation-requests",
          testRequest,
          String.class);
    }
  }

  @Test
  void camelServiceInitialization_LazyLoadingWithSynchronization() {
    // Given
    try (var mockStatic = mockStatic(CamelReactiveStreams.class)) {
      mockStatic.when(() -> CamelReactiveStreams.get(camelContext))
          .thenReturn(camelReactiveStreamsService);
      when(camelReactiveStreamsService.fromStream(anyString(), any()))
          .thenReturn(Flux.empty());

      // When - call multiple times
      reactiveMessagingService.consumeWalletRequests();
      reactiveMessagingService.consumeWalletRequests();

      // Then - CamelReactiveStreams.get should only be called once due to lazy initialization
      mockStatic.verify(() -> CamelReactiveStreams.get(camelContext), times(1));
    }
  }

  @Test
  void publishWalletRequest_WithEmptyString() {
    // Given
    String emptyRequest = "";
    Mono<String> expectedMono = Mono.just(emptyRequest);

    try (var mockStatic = mockStatic(CamelReactiveStreams.class)) {
      mockStatic.when(() -> CamelReactiveStreams.get(camelContext))
          .thenReturn(camelReactiveStreamsService);
      when(camelReactiveStreamsService.toStream(
          eq("wallet-creation-requests"),
          eq(emptyRequest),
          eq(String.class)))
          .thenReturn(expectedMono);

      // When
      Mono<Void> result = reactiveMessagingService.publishWalletRequest(emptyRequest);

      // Then
      StepVerifier.create(result)
          .verifyComplete();
    }
  }
}

