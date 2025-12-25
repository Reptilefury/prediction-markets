package com.oregonmarkets.domain.market.service.impl;

import com.oregonmarkets.common.exception.BusinessException;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.dto.mapper.MarketMapper;
import com.oregonmarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonmarkets.domain.market.dto.request.ResolveMarketRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateMarketRequest;
import com.oregonmarkets.domain.market.dto.response.MarketResponse;
import com.oregonmarkets.domain.market.dto.response.OutcomeResponse;
import com.oregonmarkets.domain.market.model.*;
import com.oregonmarkets.domain.market.repository.CategoryRepository;
import com.oregonmarkets.domain.market.repository.MarketRepository;
import com.oregonmarkets.domain.market.repository.OutcomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceImplTest {

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MarketMapper marketMapper;

    @InjectMocks
    private MarketServiceImpl marketService;

    private UUID marketId;
    private UUID categoryId;
    private UUID userId;
    private UUID outcomeId1;
    private UUID outcomeId2;
    private Market testMarket;
    private Category testCategory;
    private Outcome testOutcome1;
    private Outcome testOutcome2;
    private MarketResponse testMarketResponse;
    private OutcomeResponse testOutcomeResponse1;
    private OutcomeResponse testOutcomeResponse2;

    @BeforeEach
    void setUp() {
        marketId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        userId = UUID.randomUUID();
        outcomeId1 = UUID.randomUUID();
        outcomeId2 = UUID.randomUUID();

        testCategory = new Category();
        testCategory.setCategoryId(categoryId);
        testCategory.setName("Politics");
        testCategory.setSlug("politics");

        testMarket = new Market();
        testMarket.setMarketId(marketId);
        testMarket.setTitle("Test Market");
        testMarket.setSlug("test-market");
        testMarket.setStatus(MarketStatus.OPEN.name());
        testMarket.setCategoryId(categoryId);
        testMarket.setCreatedBy(userId);
        testMarket.setMarketClose(Instant.now().plus(7, ChronoUnit.DAYS));
        testMarket.setResolutionTime(Instant.now().plus(14, ChronoUnit.DAYS));
        testMarket.setVersion(1L);

        testOutcome1 = new Outcome();
        testOutcome1.setMarketId(marketId);
        testOutcome1.setOutcomeId(outcomeId1);
        testOutcome1.setName("Yes");
        testOutcome1.setIsWinner(false);

        testOutcome2 = new Outcome();
        testOutcome2.setMarketId(marketId);
        testOutcome2.setOutcomeId(outcomeId2);
        testOutcome2.setName("No");
        testOutcome2.setIsWinner(false);

        testMarketResponse = new MarketResponse();
        testMarketResponse.setMarketId(marketId);
        testMarketResponse.setTitle("Test Market");
        testMarketResponse.setStatus(MarketStatus.OPEN.name());

        testOutcomeResponse1 = new OutcomeResponse();
        testOutcomeResponse1.setOutcomeId(outcomeId1);
        testOutcomeResponse1.setName("Yes");

        testOutcomeResponse2 = new OutcomeResponse();
        testOutcomeResponse2.setOutcomeId(outcomeId2);
        testOutcomeResponse2.setName("No");
    }

    // ==================== Create Market Tests ====================

    @Test
    void createMarket_ValidBinaryMarket_ShouldCreateSuccessfully() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        request.setTitle("Will it happen?");
        request.setCategoryId(categoryId);
        request.setMarketType("BINARY");
        request.setMarketClose(Instant.now().plus(7, ChronoUnit.DAYS));
        request.setResolutionTime(Instant.now().plus(14, ChronoUnit.DAYS));

        CreateMarketRequest.OutcomeRequest outcome1 = new CreateMarketRequest.OutcomeRequest();
        outcome1.setName("Yes");
        CreateMarketRequest.OutcomeRequest outcome2 = new CreateMarketRequest.OutcomeRequest();
        outcome2.setName("No");
        request.setOutcomes(Arrays.asList(outcome1, outcome2));

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(marketMapper.toEntity(request, testCategory, userId)).thenReturn(testMarket);
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(marketMapper.toOutcomeEntity(eq(marketId), any())).thenReturn(testOutcome1).thenReturn(testOutcome2);
        when(outcomeRepository.save(any(Outcome.class))).thenReturn(Mono.just(testOutcome1)).thenReturn(Mono.just(testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any(Outcome.class))).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.createMarket(request, userId))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getMarketId()).isEqualTo(marketId);
                    assertThat(response.getOutcomes()).hasSize(2);
                })
                .verifyComplete();

        verify(categoryRepository).findById(categoryId);
        verify(marketRepository).save(testMarket);
        verify(outcomeRepository, times(2)).save(any(Outcome.class));
    }

    @Test
    void createMarket_CategoryNotFound_ShouldThrowException() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        request.setCategoryId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(marketService.createMarket(request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND &&
                                throwable.getMessage().contains("Category not found")
                )
                .verify();
    }

    @Test
    void createMarket_InvalidTimeRange_ShouldThrowException() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        request.setCategoryId(categoryId);
        request.setMarketClose(Instant.now().plus(14, ChronoUnit.DAYS));
        request.setResolutionTime(Instant.now().plus(7, ChronoUnit.DAYS)); // Before close time

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(marketService.createMarket(request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.INVALID_DATE_RANGE
                )
                .verify();
    }

    @Test
    void createMarket_BinaryMarketWithWrongOutcomeCount_ShouldThrowException() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        request.setCategoryId(categoryId);
        request.setMarketType("BINARY");
        request.setMarketClose(Instant.now().plus(7, ChronoUnit.DAYS));
        request.setResolutionTime(Instant.now().plus(14, ChronoUnit.DAYS));

        CreateMarketRequest.OutcomeRequest outcome1 = new CreateMarketRequest.OutcomeRequest();
        outcome1.setName("Yes");
        request.setOutcomes(Arrays.asList(outcome1)); // Only 1 outcome

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(marketService.createMarket(request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.VALIDATION_ERROR &&
                                throwable.getMessage().contains("Binary markets must have exactly 2 outcomes")
                )
                .verify();
    }

    // ==================== Get Market Tests ====================

    @Test
    void getMarketById_ExistingMarket_ShouldReturnMarket() {
        // Given
        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getMarketById(marketId))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getMarketId()).isEqualTo(marketId);
                })
                .verifyComplete();
    }

    @Test
    void getMarketById_NonExistingMarket_ShouldThrowException() {
        // Given
        when(marketRepository.findById(marketId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(marketService.getMarketById(marketId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_NOT_FOUND
                )
                .verify();
    }

    @Test
    void getMarketBySlug_ExistingMarket_ShouldReturnMarket() {
        // Given
        String slug = "test-market";
        when(marketRepository.findBySlug(slug)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getMarketBySlug(slug))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getMarketId()).isEqualTo(marketId);
                })
                .verifyComplete();
    }

    @Test
    void getMarketBySlug_NonExistingMarket_ShouldThrowException() {
        // Given
        String slug = "non-existing";
        when(marketRepository.findBySlug(slug)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(marketService.getMarketBySlug(slug))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_NOT_FOUND
                )
                .verify();
    }

    @Test
    void getAllMarkets_ShouldReturnAllMarkets() {
        // Given
        when(marketRepository.findAll()).thenReturn(Flux.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getAllMarkets())
                .assertNext(response -> assertThat(response.getMarketId()).isEqualTo(marketId))
                .verifyComplete();
    }

    @Test
    void getMarketsByCategory_ShouldReturnFilteredMarkets() {
        // Given
        when(marketRepository.findByCategoryId(categoryId)).thenReturn(Flux.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getMarketsByCategory(categoryId))
                .assertNext(response -> assertThat(response.getMarketId()).isEqualTo(marketId))
                .verifyComplete();
    }

    @Test
    void getMarketsByStatus_ShouldReturnFilteredMarkets() {
        // Given
        String status = "OPEN";
        when(marketRepository.findByStatus(status)).thenReturn(Flux.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getMarketsByStatus(status))
                .assertNext(response -> assertThat(response.getMarketId()).isEqualTo(marketId))
                .verifyComplete();
    }

    @Test
    void getFeaturedMarkets_ShouldReturnFeaturedMarkets() {
        // Given
        testMarket.setFeatured(true);
        when(marketRepository.findByFeaturedTrue()).thenReturn(Flux.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getFeaturedMarkets())
                .assertNext(response -> assertThat(response.getMarketId()).isEqualTo(marketId))
                .verifyComplete();
    }

    @Test
    void getTrendingMarkets_ShouldReturnTrendingMarkets() {
        // Given
        testMarket.setTrending(true);
        when(marketRepository.findByTrendingTrue()).thenReturn(Flux.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getTrendingMarkets())
                .assertNext(response -> assertThat(response.getMarketId()).isEqualTo(marketId))
                .verifyComplete();
    }

    // ==================== Update Market Tests ====================

    @Test
    void updateMarket_ValidUpdate_ShouldUpdateSuccessfully() {
        // Given
        UpdateMarketRequest request = new UpdateMarketRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(marketMapper.generateSlug(anyString(), any(UUID.class))).thenReturn("updated-title");
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.updateMarket(marketId, request, userId))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        verify(marketRepository).save(testMarket);
    }

    @Test
    void updateMarket_ResolvedMarket_ShouldThrowException() {
        // Given
        testMarket.setStatus(MarketStatus.RESOLVED.name());
        UpdateMarketRequest request = new UpdateMarketRequest();

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));

        // When & Then
        StepVerifier.create(marketService.updateMarket(marketId, request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_ALREADY_RESOLVED
                )
                .verify();
    }

    @Test
    void updateMarket_CancelledMarket_ShouldThrowException() {
        // Given
        testMarket.setStatus(MarketStatus.CANCELLED.name());
        UpdateMarketRequest request = new UpdateMarketRequest();

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));

        // When & Then
        StepVerifier.create(marketService.updateMarket(marketId, request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_ALREADY_RESOLVED
                )
                .verify();
    }

    @Test
    void updateMarket_NonExistingMarket_ShouldThrowException() {
        // Given
        UpdateMarketRequest request = new UpdateMarketRequest();
        when(marketRepository.findById(marketId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(marketService.updateMarket(marketId, request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_NOT_FOUND
                )
                .verify();
    }

    // ==================== Resolve Market Tests ====================

    @Test
    void resolveMarket_ValidResolution_ShouldResolveSuccessfully() {
        // Given
        testMarket.setStatus(MarketStatus.CLOSED.name());
        ResolveMarketRequest request = new ResolveMarketRequest();
        request.setWinningOutcomeId(outcomeId1);
        request.setResolutionNotes("Clear winner");

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketIdAndOutcomeId(marketId, outcomeId1)).thenReturn(Mono.just(testOutcome1));
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.save(testOutcome1)).thenReturn(Mono.just(testOutcome1));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.resolveMarket(marketId, request, userId))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        verify(marketRepository).save(testMarket);
        verify(outcomeRepository).save(testOutcome1);
    }

    @Test
    void resolveMarket_AlreadyResolved_ShouldThrowException() {
        // Given
        testMarket.setStatus(MarketStatus.RESOLVED.name());
        ResolveMarketRequest request = new ResolveMarketRequest();

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));

        // When & Then
        StepVerifier.create(marketService.resolveMarket(marketId, request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_ALREADY_RESOLVED
                )
                .verify();
    }

    @Test
    void resolveMarket_NotClosedYet_ShouldThrowException() {
        // Given
        testMarket.setStatus(MarketStatus.OPEN.name());
        ResolveMarketRequest request = new ResolveMarketRequest();

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));

        // When & Then
        StepVerifier.create(marketService.resolveMarket(marketId, request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.MARKET_CLOSED
                )
                .verify();
    }

    @Test
    void resolveMarket_InvalidWinningOutcome_ShouldThrowException() {
        // Given
        testMarket.setStatus(MarketStatus.CLOSED.name());
        ResolveMarketRequest request = new ResolveMarketRequest();
        request.setWinningOutcomeId(UUID.randomUUID());

        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketIdAndOutcomeId(eq(marketId), any(UUID.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(marketService.resolveMarket(marketId, request, userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND &&
                                throwable.getMessage().contains("Winning outcome not found")
                )
                .verify();
    }

    // ==================== Status Change Tests ====================

    @Test
    void closeMarket_ValidMarket_ShouldCloseSuccessfully() {
        // Given
        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.closeMarket(marketId, userId))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        assertThat(testMarket.getStatus()).isEqualTo(MarketStatus.CLOSED.name());
    }

    @Test
    void suspendMarket_ValidMarket_ShouldSuspendSuccessfully() {
        // Given
        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.suspendMarket(marketId, "Suspicious activity", userId))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        assertThat(testMarket.getStatus()).isEqualTo(MarketStatus.SUSPENDED.name());
    }

    @Test
    void reopenMarket_ValidMarket_ShouldReopenSuccessfully() {
        // Given
        testMarket.setStatus(MarketStatus.CLOSED.name());
        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.reopenMarket(marketId, userId))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        assertThat(testMarket.getStatus()).isEqualTo(MarketStatus.OPEN.name());
    }

    @Test
    void cancelMarket_ValidMarket_ShouldCancelSuccessfully() {
        // Given
        when(marketRepository.findById(marketId)).thenReturn(Mono.just(testMarket));
        when(marketRepository.save(testMarket)).thenReturn(Mono.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.cancelMarket(marketId, "Invalid market", userId))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        assertThat(testMarket.getStatus()).isEqualTo(MarketStatus.CANCELLED.name());
    }

    // ==================== Outcome Tests ====================

    @Test
    void getMarketOutcomes_ShouldReturnAllOutcomes() {
        // Given
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.getMarketOutcomes(marketId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void getOutcome_ExistingOutcome_ShouldReturnOutcome() {
        // Given
        when(outcomeRepository.findByMarketIdAndOutcomeId(marketId, outcomeId1)).thenReturn(Mono.just(testOutcome1));
        when(marketMapper.toOutcomeResponse(testOutcome1)).thenReturn(testOutcomeResponse1);

        // When & Then
        StepVerifier.create(marketService.getOutcome(marketId, outcomeId1))
                .assertNext(response -> assertThat(response.getOutcomeId()).isEqualTo(outcomeId1))
                .verifyComplete();
    }

    @Test
    void getOutcome_NonExistingOutcome_ShouldThrowException() {
        // Given
        when(outcomeRepository.findByMarketIdAndOutcomeId(marketId, outcomeId1)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(marketService.getOutcome(marketId, outcomeId1))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();
    }

    // ==================== Search Tests ====================

    @Test
    void searchMarkets_ShouldReturnMatchingMarkets() {
        // Given
        String query = "test";
        testMarket.setTitle("Test Market");
        testMarket.setDescription("This is a test description");

        when(marketRepository.findAll()).thenReturn(Flux.just(testMarket));
        when(outcomeRepository.findByMarketId(marketId)).thenReturn(Flux.just(testOutcome1, testOutcome2));
        when(marketMapper.toResponse(testMarket)).thenReturn(testMarketResponse);
        when(marketMapper.toOutcomeResponse(any())).thenReturn(testOutcomeResponse1, testOutcomeResponse2);

        // When & Then
        StepVerifier.create(marketService.searchMarkets(query))
                .assertNext(response -> assertThat(response.getMarketId()).isEqualTo(marketId))
                .verifyComplete();
    }

    @Test
    void searchMarkets_NoMatch_ShouldReturnEmpty() {
        // Given
        String query = "nonexistent";
        testMarket.setTitle("Test Market");
        testMarket.setDescription("This is a test");

        when(marketRepository.findAll()).thenReturn(Flux.just(testMarket));

        // When & Then
        StepVerifier.create(marketService.searchMarkets(query))
                .verifyComplete();
    }
}
