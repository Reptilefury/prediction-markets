package com.oregonMarkets.domain.market.service.impl;

import com.oregonMarkets.common.exception.BusinessException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.domain.market.dto.mapper.MarketMapper;
import com.oregonMarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonMarkets.domain.market.dto.request.ResolveMarketRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateMarketRequest;
import com.oregonMarkets.domain.market.dto.response.MarketResponse;
import com.oregonMarkets.domain.market.dto.response.OutcomeResponse;
import com.oregonMarkets.domain.market.model.*;
import com.oregonMarkets.domain.market.repository.CategoryRepository;
import com.oregonMarkets.domain.market.repository.MarketRepository;
import com.oregonMarkets.domain.market.repository.OutcomeRepository;
import com.oregonMarkets.domain.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of MarketService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final CategoryRepository categoryRepository;
    private final MarketMapper marketMapper;

    @Override
    public Mono<MarketResponse> createMarket(CreateMarketRequest request, UUID createdBy) {
        log.info("Creating market: {} by user: {}", request.getTitle(), createdBy);

        return validateMarketCreation(request)
                .flatMap(category -> {
                    // Create market entity using mapper
                    Market market = marketMapper.toEntity(request, category, createdBy);

                    // Save market
                    return marketRepository.save(market)
                            .flatMap(savedMarket -> {
                                // Create outcomes using mapper
                                Flux<Outcome> outcomes = Flux.fromIterable(request.getOutcomes())
                                        .map(outcomeReq -> marketMapper.toOutcomeEntity(savedMarket.getMarketId(), outcomeReq))
                                        .flatMap(outcomeRepository::save);

                                return outcomes.collectList()
                                        .map(outcomeList -> {
                                            MarketResponse response = marketMapper.toResponse(savedMarket);
                                            response.setOutcomes(outcomeList.stream()
                                                    .map(marketMapper::toOutcomeResponse)
                                                    .collect(Collectors.toList()));
                                            return response;
                                        });
                            });
                })
                .doOnSuccess(response -> log.info("Market created successfully: {}", response.getMarketId()))
                .doOnError(error -> log.error("Failed to create market: {}", request.getTitle(), error));
    }

    @Override
    public Mono<MarketResponse> getMarketById(UUID marketId) {
        log.debug("Fetching market by ID: {}", marketId);

        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market ->
                    outcomeRepository.findByMarketId(marketId)
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Mono<MarketResponse> getMarketBySlug(String slug) {
        log.debug("Fetching market by slug: {}", slug);

        return marketRepository.findBySlug(slug)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with slug: " + slug
                )))
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Flux<MarketResponse> getAllMarkets() {
        log.debug("Fetching all markets");

        return marketRepository.findAll()
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Flux<MarketResponse> getMarketsByCategory(UUID categoryId) {
        log.debug("Fetching markets by category: {}", categoryId);

        return marketRepository.findByCategoryId(categoryId)
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Flux<MarketResponse> getMarketsByStatus(String status) {
        log.debug("Fetching markets by status: {}", status);

        return marketRepository.findByStatus(status)
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Flux<MarketResponse> getFeaturedMarkets() {
        log.debug("Fetching featured markets");

        return marketRepository.findByFeaturedTrue()
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Flux<MarketResponse> getTrendingMarkets() {
        log.debug("Fetching trending markets");

        return marketRepository.findByTrendingTrue()
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    @Override
    public Mono<MarketResponse> updateMarket(UUID marketId, UpdateMarketRequest request, UUID updatedBy) {
        log.info("Updating market: {} by user: {}", marketId, updatedBy);

        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market -> {
                    // Validate market can be updated
                    if ("RESOLVED".equals(market.getStatus()) || "CANCELLED".equals(market.getStatus())) {
                        return Mono.error(new BusinessException(
                                ResponseCode.MARKET_ALREADY_RESOLVED,
                                "Cannot update resolved or cancelled market"
                        ));
                    }

                    // Apply updates
                    applyUpdates(market, request, updatedBy);

                    return marketRepository.save(market);
                })
                .flatMap(updatedMarket ->
                    outcomeRepository.findByMarketId(updatedMarket.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(updatedMarket);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                )
                .doOnSuccess(response -> log.info("Market updated successfully: {}", marketId))
                .doOnError(error -> log.error("Failed to update market: {}", marketId, error));
    }

    @Override
    public Mono<MarketResponse> resolveMarket(UUID marketId, ResolveMarketRequest request, UUID resolvedBy) {
        log.info("Resolving market: {} by user: {}", marketId, resolvedBy);

        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market -> {
                    // Validate market can be resolved
                    if ("RESOLVED".equals(market.getStatus())) {
                        return Mono.error(new BusinessException(
                                ResponseCode.MARKET_ALREADY_RESOLVED,
                                "Market is already resolved"
                        ));
                    }

                    if (!"CLOSED".equals(market.getStatus())) {
                        return Mono.error(new BusinessException(
                                ResponseCode.MARKET_CLOSED,
                                "Market must be closed before resolution"
                        ));
                    }

                    // Verify winning outcome exists
                    return outcomeRepository.findByMarketIdAndOutcomeId(marketId, request.getWinningOutcomeId())
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    ResponseCode.NOT_FOUND,
                                    "Winning outcome not found"
                            )))
                            .flatMap(winningOutcome -> {
                                // Update market
                                market.setStatus(MarketStatus.RESOLVED.name());
                                market.setWinningOutcomeId(request.getWinningOutcomeId());
                                market.setWinningOutcomeName(winningOutcome.getName());
                                market.setResolutionNotes(request.getResolutionNotes());
                                market.setResolvedBy(resolvedBy);
                                market.setActualResolutionTime(Instant.now());
                                market.setUpdatedAt(Instant.now());
                                market.setUpdatedBy(resolvedBy);

                                // Update winning outcome
                                winningOutcome.setIsWinner(true);
                                winningOutcome.setUpdatedAt(Instant.now());

                                return marketRepository.save(market)
                                        .then(outcomeRepository.save(winningOutcome))
                                        .thenReturn(market);
                            });
                })
                .flatMap(resolvedMarket ->
                    outcomeRepository.findByMarketId(resolvedMarket.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(resolvedMarket);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                )
                .doOnSuccess(response -> log.info("Market resolved successfully: {}", marketId))
                .doOnError(error -> log.error("Failed to resolve market: {}", marketId, error));
    }

    @Override
    public Mono<MarketResponse> closeMarket(UUID marketId, UUID closedBy) {
        log.info("Closing market: {} by user: {}", marketId, closedBy);

        return updateMarketStatus(marketId, MarketStatus.CLOSED.name(), closedBy);
    }

    @Override
    public Mono<MarketResponse> suspendMarket(UUID marketId, String reason, UUID suspendedBy) {
        log.info("Suspending market: {} by user: {}", marketId, suspendedBy);

        return updateMarketStatus(marketId, MarketStatus.SUSPENDED.name(), suspendedBy);
    }

    @Override
    public Mono<MarketResponse> reopenMarket(UUID marketId, UUID reopenedBy) {
        log.info("Reopening market: {} by user: {}", marketId, reopenedBy);

        return updateMarketStatus(marketId, MarketStatus.OPEN.name(), reopenedBy);
    }

    @Override
    public Mono<MarketResponse> cancelMarket(UUID marketId, String reason, UUID cancelledBy) {
        log.info("Cancelling market: {} by user: {}", marketId, cancelledBy);

        return updateMarketStatus(marketId, MarketStatus.CANCELLED.name(), cancelledBy);
    }

    @Override
    public Flux<OutcomeResponse> getMarketOutcomes(UUID marketId) {
        log.debug("Fetching outcomes for market: {}", marketId);

        return outcomeRepository.findByMarketId(marketId)
                .map(marketMapper::toOutcomeResponse);
    }

    @Override
    public Mono<OutcomeResponse> getOutcome(UUID marketId, UUID outcomeId) {
        log.debug("Fetching outcome: {} for market: {}", outcomeId, marketId);

        return outcomeRepository.findByMarketIdAndOutcomeId(marketId, outcomeId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Outcome not found"
                )))
                .map(marketMapper::toOutcomeResponse);
    }

    @Override
    public Flux<MarketResponse> searchMarkets(String query) {
        log.debug("Searching markets with query: {}", query);

        // Simple implementation - in production, use full-text search or Elasticsearch
        return marketRepository.findAll()
                .filter(market ->
                    market.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    (market.getDescription() != null && market.getDescription().toLowerCase().contains(query.toLowerCase()))
                )
                .flatMap(market ->
                    outcomeRepository.findByMarketId(market.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(market);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }

    // ==================== Private Helper Methods ====================

    private Mono<Category> validateMarketCreation(CreateMarketRequest request) {
        return categoryRepository.findById(request.getCategoryId())
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Category not found with ID: " + request.getCategoryId()
                )))
                .flatMap(category -> {
                    // Validate market close time is before resolution time
                    if (request.getMarketClose().isAfter(request.getResolutionTime())) {
                        return Mono.error(new BusinessException(
                                ResponseCode.INVALID_DATE_RANGE,
                                "Market close time must be before resolution time"
                        ));
                    }

                    // Validate market type has correct number of outcomes
                    if ("BINARY".equals(request.getMarketType()) && request.getOutcomes().size() != 2) {
                        return Mono.error(new BusinessException(
                                ResponseCode.VALIDATION_ERROR,
                                "Binary markets must have exactly 2 outcomes"
                        ));
                    }

                    return Mono.just(category);
                });
    }


    private void applyUpdates(Market market, UpdateMarketRequest request, UUID updatedBy) {
        Instant now = Instant.now();

        if (request.getTitle() != null) {
            market.setTitle(request.getTitle());
            market.setSlug(marketMapper.generateSlug(request.getTitle(), market.getMarketId()));
        }
        if (request.getDescription() != null) market.setDescription(request.getDescription());
        if (request.getLongDescription() != null) market.setLongDescription(request.getLongDescription());
        if (request.getStatus() != null) market.setStatus(request.getStatus());
        if (request.getMarketClose() != null) market.setMarketClose(request.getMarketClose());
        if (request.getResolutionTime() != null) market.setResolutionTime(request.getResolutionTime());
        if (request.getResolutionCriteria() != null) market.setResolutionCriteria(request.getResolutionCriteria());
        if (request.getImageUrl() != null) market.setImageUrl(request.getImageUrl());
        if (request.getBannerUrl() != null) market.setBannerUrl(request.getBannerUrl());
        if (request.getFeatured() != null) market.setFeatured(request.getFeatured());
        if (request.getTrending() != null) market.setTrending(request.getTrending());
        if (request.getDisplayPriority() != null) market.setDisplayPriority(request.getDisplayPriority());
        if (request.getTags() != null) market.setTags(request.getTags());
        if (request.getRestrictedCountries() != null) market.setRestrictedCountries(request.getRestrictedCountries());
        if (request.getKycRequired() != null) market.setKycRequired(request.getKycRequired());
        if (request.getMinAge() != null) market.setMinAge(request.getMinAge());
        if (request.getViewTemplateId() != null) market.setViewTemplateId(request.getViewTemplateId());

        market.setUpdatedAt(now);
        market.setUpdatedBy(updatedBy);
        market.setVersion(market.getVersion() + 1);
    }

    private Mono<MarketResponse> updateMarketStatus(UUID marketId, String newStatus, UUID updatedBy) {
        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market -> {
                    market.setStatus(newStatus);
                    market.setUpdatedAt(Instant.now());
                    market.setUpdatedBy(updatedBy);
                    market.setVersion(market.getVersion() + 1);

                    return marketRepository.save(market);
                })
                .flatMap(updatedMarket ->
                    outcomeRepository.findByMarketId(updatedMarket.getMarketId())
                            .collectList()
                            .map(outcomes -> {
                                MarketResponse response = marketMapper.toResponse(updatedMarket);
                                response.setOutcomes(outcomes.stream()
                                        .map(marketMapper::toOutcomeResponse)
                                        .collect(Collectors.toList()));
                                return response;
                            })
                );
    }
}
