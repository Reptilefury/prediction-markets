package com.oregonmarkets.domain.market.service;

import com.oregonmarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateFeaturedMarketConfigRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateMarketViewConfigRequest;
import com.oregonmarkets.domain.market.dto.request.ResolveMarketRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateMarketRequest;
import com.oregonmarkets.domain.market.dto.response.MarketResponse;
import com.oregonmarkets.domain.market.dto.response.MarketViewPreviewResponse;
import com.oregonmarkets.domain.market.dto.response.OutcomeResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service interface for market operations
 */
public interface MarketService {

    /**
     * Create a new market
     */
    Mono<MarketResponse> createMarket(CreateMarketRequest request, UUID createdBy);

    /**
     * Get market by ID
     */
    Mono<MarketResponse> getMarketById(UUID marketId);

    /**
     * Get market by slug
     */
    Mono<MarketResponse> getMarketBySlug(String slug);

    /**
     * Get all markets (paginated)
     */
    Flux<MarketResponse> getAllMarkets();

    /**
     * Get markets by category
     */
    Flux<MarketResponse> getMarketsByCategory(UUID categoryId);

    /**
     * Get markets by status
     */
    Flux<MarketResponse> getMarketsByStatus(String status);

    /**
     * Get featured markets
     */
    Flux<MarketResponse> getFeaturedMarkets();

    /**
     * Get trending markets
     */
    Flux<MarketResponse> getTrendingMarkets();

    /**
     * Update market
     */
    Mono<MarketResponse> updateMarket(UUID marketId, UpdateMarketRequest request, UUID updatedBy);

    /**
     * Resolve market with winning outcome
     */
    Mono<MarketResponse> resolveMarket(UUID marketId, ResolveMarketRequest request, UUID resolvedBy);

    /**
     * Close market for trading
     */
    Mono<MarketResponse> closeMarket(UUID marketId, UUID closedBy);

    /**
     * Suspend market trading
     */
    Mono<MarketResponse> suspendMarket(UUID marketId, String reason, UUID suspendedBy);

    /**
     * Reopen suspended market
     */
    Mono<MarketResponse> reopenMarket(UUID marketId, UUID reopenedBy);

    /**
     * Cancel market
     */
    Mono<MarketResponse> cancelMarket(UUID marketId, String reason, UUID cancelledBy);

    /**
     * Get outcomes for a market
     */
    Flux<OutcomeResponse> getMarketOutcomes(UUID marketId);

    /**
     * Get specific outcome
     */
    Mono<OutcomeResponse> getOutcome(UUID marketId, UUID outcomeId);

    /**
     * Search markets by title or description
     */
    Flux<MarketResponse> searchMarkets(String query);

    /**
     * Update market view configuration
     */
    Mono<MarketResponse> updateMarketViewConfig(UUID marketId, UpdateMarketViewConfigRequest request, UUID updatedBy);

    /**
     * Update featured market configuration
     */
    Mono<MarketResponse> updateFeaturedMarketConfig(UUID marketId, UpdateFeaturedMarketConfigRequest request, UUID updatedBy);

    /**
     * Preview market view configuration
     */
    Mono<MarketViewPreviewResponse> getMarketViewPreview(UUID marketId);
}
