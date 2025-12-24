package com.oregonMarkets.domain.market.handler;

import com.oregonMarkets.common.exception.BusinessException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateMarketRequest;
import com.oregonMarkets.domain.market.dto.response.MarketResponse;
import com.oregonMarkets.domain.market.service.MarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketHandlerTest {

    @Mock
    private MarketService marketService;

    @Mock
    private ServerRequest serverRequest;

    @InjectMocks
    private MarketHandler marketHandler;

    private UUID testMarketId;
    private MarketResponse testMarketResponse;

    @BeforeEach
    void setUp() {
        testMarketId = UUID.randomUUID();
        testMarketResponse = new MarketResponse();
        testMarketResponse.setMarketId(testMarketId);
        testMarketResponse.setTitle("Test Market");
    }

    @Test
    void createMarket_ValidRequest_ShouldReturnCreated() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        when(serverRequest.bodyToMono(CreateMarketRequest.class)).thenReturn(Mono.just(request));
        when(marketService.createMarket(any(), any())).thenReturn(Mono.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.createMarket(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(marketService).createMarket(eq(request), any());
    }

    @Test
    void createMarket_ServiceError_ShouldHandleError() {
        // Given
        when(serverRequest.bodyToMono(CreateMarketRequest.class)).thenReturn(Mono.just(new CreateMarketRequest()));
        when(marketService.createMarket(any(), any()))
                .thenReturn(Mono.error(new BusinessException(ResponseCode.VALIDATION_ERROR, "Validation failed")));

        // When
        Mono<ServerResponse> response = marketHandler.createMarket(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getMarketById_ValidId_ShouldReturnMarket() {
        // Given
        when(serverRequest.pathVariable("marketId")).thenReturn(testMarketId.toString());
        when(marketService.getMarketById(testMarketId)).thenReturn(Mono.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getMarketById(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getMarketById_InvalidId_ShouldReturnBadRequest() {
        // Given
        when(serverRequest.pathVariable("marketId")).thenReturn("invalid-uuid");

        // When
        Mono<ServerResponse> response = marketHandler.getMarketById(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getMarketBySlug_ValidSlug_ShouldReturnMarket() {
        // Given
        String slug = "test-market";
        when(serverRequest.pathVariable("slug")).thenReturn(slug);
        when(marketService.getMarketBySlug(slug)).thenReturn(Mono.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getMarketBySlug(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getAllMarkets_ShouldReturnListOfMarkets() {
        // Given
        when(marketService.getAllMarkets()).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getAllMarkets(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getMarketsByCategory_ValidCategoryId_ShouldReturnMarkets() {
        // Given
        UUID categoryId = UUID.randomUUID();
        when(serverRequest.queryParam("category")).thenReturn(Optional.of(categoryId.toString()));
        when(marketService.getMarketsByCategory(categoryId)).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getMarketsByCategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getMarketsByCategory_NoParameter_ShouldReturnAllMarkets() {
        // Given
        when(serverRequest.queryParam("category")).thenReturn(Optional.empty());
        when(marketService.getAllMarkets()).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getMarketsByCategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getMarketsByStatus_ValidStatus_ShouldReturnMarkets() {
        // Given
        when(serverRequest.queryParam("status")).thenReturn(Optional.of("OPEN"));
        when(marketService.getMarketsByStatus("OPEN")).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getMarketsByStatus(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getFeaturedMarkets_ShouldReturnFeaturedMarkets() {
        // Given
        when(marketService.getFeaturedMarkets()).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getFeaturedMarkets(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getTrendingMarkets_ShouldReturnTrendingMarkets() {
        // Given
        when(marketService.getTrendingMarkets()).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.getTrendingMarkets(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void updateMarket_ValidRequest_ShouldReturnUpdatedMarket() {
        // Given
        UpdateMarketRequest request = new UpdateMarketRequest();
        when(serverRequest.pathVariable("marketId")).thenReturn(testMarketId.toString());
        when(serverRequest.bodyToMono(UpdateMarketRequest.class)).thenReturn(Mono.just(request));
        when(marketService.updateMarket(eq(testMarketId), eq(request), any())).thenReturn(Mono.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.updateMarket(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void closeMarket_ValidId_ShouldCloseMarket() {
        // Given
        when(serverRequest.pathVariable("marketId")).thenReturn(testMarketId.toString());
        when(marketService.closeMarket(eq(testMarketId), any())).thenReturn(Mono.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.closeMarket(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void searchMarkets_WithQuery_ShouldReturnResults() {
        // Given
        when(serverRequest.queryParam("q")).thenReturn(Optional.of("test"));
        when(marketService.searchMarkets("test")).thenReturn(Flux.just(testMarketResponse));

        // When
        Mono<ServerResponse> response = marketHandler.searchMarkets(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void searchMarkets_WithoutQuery_ShouldReturnBadRequest() {
        // Given
        when(serverRequest.queryParam("q")).thenReturn(Optional.empty());

        // When
        Mono<ServerResponse> response = marketHandler.searchMarkets(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 400)
                .verifyComplete();
    }
}
