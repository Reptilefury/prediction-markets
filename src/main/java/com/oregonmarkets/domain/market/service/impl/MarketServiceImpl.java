package com.oregonmarkets.domain.market.service.impl;

import com.oregonmarkets.common.exception.BusinessException;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.dto.mapper.MarketMapper;
import com.oregonmarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonmarkets.domain.market.dto.request.ResolveMarketRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateFeaturedMarketConfigRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateMarketViewConfigRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateMarketRequest;
import com.oregonmarkets.domain.market.dto.response.MarketResponse;
import com.oregonmarkets.domain.market.dto.response.MarketViewPreviewResponse;
import com.oregonmarkets.domain.market.dto.response.OutcomeResponse;
import com.oregonmarkets.domain.market.model.*;
import com.oregonmarkets.domain.market.repository.CategoryRepository;
import com.oregonmarkets.domain.market.repository.MarketTypeRepository;
import com.oregonmarkets.domain.market.repository.MarketRepository;
import com.oregonmarkets.domain.market.repository.OutcomeRepository;
import com.oregonmarkets.domain.market.repository.ViewTemplateRepository;
import com.oregonmarkets.domain.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Objects;
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
    private final MarketTypeRepository marketTypeRepository;
    private final ViewTemplateRepository viewTemplateRepository;
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
                .flatMap(this::enrichMarketWithOutcomes);
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

    @Override
    public Mono<MarketResponse> updateMarketViewConfig(UUID marketId, UpdateMarketViewConfigRequest request, UUID updatedBy) {
        log.info("Updating market view config: {}", marketId);

        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market -> {
                    if (request.getViewTemplateId() != null) {
                        market.setViewTemplateId(request.getViewTemplateId());
                    }
                    if (request.getViewConfigOverride() != null) {
                        market.setViewConfig(request.getViewConfigOverride());
                    }
                    market.setUpdatedAt(Instant.now());
                    market.setUpdatedBy(updatedBy);
                    market.setVersion(market.getVersion() + 1);
                    return marketRepository.save(market);
                })
                .flatMap(this::enrichMarketWithOutcomes);
    }

    @Override
    public Mono<MarketResponse> updateFeaturedMarketConfig(UUID marketId, UpdateFeaturedMarketConfigRequest request, UUID updatedBy) {
        log.info("Updating featured market config: {}", marketId);

        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market -> {
                    if (request.getFeatured() != null) {
                        market.setFeatured(request.getFeatured());
                    }
                    if (request.getFeaturedViewTemplateId() != null) {
                        market.setFeaturedViewTemplateId(request.getFeaturedViewTemplateId());
                    }
                    if (request.getFeaturedViewConfigOverride() != null) {
                        market.setFeaturedViewConfig(request.getFeaturedViewConfigOverride());
                    }
                    if (request.getFeaturedRank() != null) {
                        market.setFeaturedRank(request.getFeaturedRank());
                    }
                    market.setUpdatedAt(Instant.now());
                    market.setUpdatedBy(updatedBy);
                    market.setVersion(market.getVersion() + 1);
                    return marketRepository.save(market);
                })
                .flatMap(this::enrichMarketWithOutcomes);
    }

    @Override
    public Mono<MarketViewPreviewResponse> getMarketViewPreview(UUID marketId) {
        log.debug("Fetching view preview for market: {}", marketId);

        return marketRepository.findById(marketId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.MARKET_NOT_FOUND,
                        "Market not found with ID: " + marketId
                )))
                .flatMap(market -> {
                    Mono<MarketResponse> marketResponseMono = enrichMarketWithOutcomes(market);
                    Mono<java.util.Optional<ViewTemplate>> viewTemplateMono = market.getViewTemplateId() == null
                            ? Mono.just(java.util.Optional.empty())
                            : viewTemplateRepository.findById(market.getViewTemplateId())
                                    .map(java.util.Optional::of)
                                    .defaultIfEmpty(java.util.Optional.empty());
                    Mono<java.util.Optional<ViewTemplate>> featuredTemplateMono = market.getFeaturedViewTemplateId() == null
                            ? Mono.just(java.util.Optional.empty())
                            : viewTemplateRepository.findById(market.getFeaturedViewTemplateId())
                                    .map(java.util.Optional::of)
                                    .defaultIfEmpty(java.util.Optional.empty());

                    return Mono.zip(marketResponseMono, viewTemplateMono, featuredTemplateMono)
                            .map(tuple -> {
                                MarketResponse response = tuple.getT1();
                                ViewTemplate viewTemplate = tuple.getT2().orElse(null);
                                ViewTemplate featuredTemplate = tuple.getT3().orElse(null);

                                String resolvedViewConfig = mergeJsonConfig(
                                        viewTemplate == null ? null : viewTemplate.getJsonConfig(),
                                        market.getViewConfig()
                                );

                                String resolvedFeaturedConfig = mergeJsonConfig(
                                        featuredTemplate == null ? null : featuredTemplate.getJsonConfig(),
                                        market.getFeaturedViewConfig()
                                );

                                return MarketViewPreviewResponse.builder()
                                        .market(response)
                                        .viewTemplate(viewTemplate)
                                        .resolvedViewConfig(resolvedViewConfig)
                                        .featuredViewTemplate(featuredTemplate)
                                        .resolvedFeaturedViewConfig(resolvedFeaturedConfig)
                                        .build();
                            });
                });
    }

    // ==================== Private Helper Methods ====================

    /**
     * Helper method to enrich market with outcomes and convert to response DTO.
     * Eliminates duplication of outcome-fetching logic across multiple methods.
     *
     * @param market The market entity to enrich
     * @return Mono containing the enriched MarketResponse with all outcomes
     */
    private Mono<MarketResponse> enrichMarketWithOutcomes(Market market) {
        return outcomeRepository.findByMarketId(market.getMarketId())
                .collectList()
                .map(outcomes -> {
                    MarketResponse response = marketMapper.toResponse(market);
                    response.setOutcomes(outcomes.stream()
                            .map(marketMapper::toOutcomeResponse)
                            .collect(Collectors.toList()));
                    return response;
                });
    }

    private String mergeJsonConfig(String baseConfig, String overrideConfig) {
        if (overrideConfig == null || overrideConfig.isBlank()) {
            return baseConfig;
        }
        if (baseConfig == null || baseConfig.isBlank()) {
            return overrideConfig;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode baseNode = mapper.readTree(baseConfig);
            JsonNode overrideNode = mapper.readTree(overrideConfig);
            if (baseNode instanceof ObjectNode && overrideNode instanceof ObjectNode) {
                ((ObjectNode) baseNode).setAll((ObjectNode) overrideNode);
                return mapper.writeValueAsString(baseNode);
            }
            return overrideConfig;
        } catch (Exception error) {
            log.warn("Failed to merge view config JSON, using override only", error);
            return Objects.requireNonNullElse(overrideConfig, baseConfig);
        }
    }

    /**
     * Helper method to enrich multiple markets with outcomes and convert to response DTOs.
     * Used in Flux-based methods.
     *
     * @param market The market entity to enrich
     * @return Mono containing the enriched MarketResponse with all outcomes
     */
    private Mono<MarketResponse> flatMapWithOutcomes(Market market) {
        return enrichMarketWithOutcomes(market);
    }

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

                    if (request.getMarketType() == null) {
                        return Mono.error(new BusinessException(
                                ResponseCode.MISSING_REQUIRED_FIELD,
                                "Market type is required"
                        ));
                    }

                    return marketTypeRepository.findById(request.getMarketType())
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    ResponseCode.INVALID_INPUT,
                                    "Market type not found: " + request.getMarketType()
                            )))
                            .flatMap(marketType -> {
                                int outcomeCount = request.getOutcomes() == null ? 0 : request.getOutcomes().size();

                                // Preserve binary validation message expected by tests
                                if ("BINARY".equals(request.getMarketType()) && outcomeCount != 2) {
                                    return Mono.error(new BusinessException(
                                            ResponseCode.VALIDATION_ERROR,
                                            "Binary markets must have exactly 2 outcomes"
                                    ));
                                }

                                Integer minOutcomes = marketType.getMinOutcomes();
                                Integer maxOutcomes = marketType.getMaxOutcomes();

                                if (minOutcomes != null && minOutcomes > 0 && outcomeCount < minOutcomes) {
                                    return Mono.error(new BusinessException(
                                            ResponseCode.VALIDATION_ERROR,
                                            "Market type " + request.getMarketType() + " requires at least " + minOutcomes + " outcomes"
                                    ));
                                }

                                if (maxOutcomes != null && maxOutcomes > 0 && outcomeCount > maxOutcomes) {
                                    return Mono.error(new BusinessException(
                                            ResponseCode.VALIDATION_ERROR,
                                            "Market type " + request.getMarketType() + " allows at most " + maxOutcomes + " outcomes"
                                    ));
                                }

                                return Mono.just(category);
                            });
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
        if (request.getViewConfig() != null) market.setViewConfig(request.getViewConfig());

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
