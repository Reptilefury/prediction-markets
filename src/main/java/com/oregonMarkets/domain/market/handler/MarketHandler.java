package com.oregonMarkets.domain.market.handler;

import com.oregonMarkets.common.response.ApiResponse;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonMarkets.domain.market.dto.request.ResolveMarketRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateMarketRequest;
import com.oregonMarkets.domain.market.dto.response.MarketResponse;
import com.oregonMarkets.domain.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Functional handler for market API endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketHandler {

    private final MarketService marketService;

    /**
     * POST /api/v1/markets - Create a new market
     */
    public Mono<ServerResponse> createMarket(ServerRequest request) {
        // TODO: Extract user ID from authentication context
        UUID createdBy = UUID.randomUUID(); // Placeholder
        return request.bodyToMono(CreateMarketRequest.class)
                .flatMap(req -> marketService.createMarket(req, createdBy))
                .flatMap(market -> {
                    ApiResponse<MarketResponse> response = ApiResponse.success(
                            ResponseCode.MARKET_CREATED,
                            market
                    );
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * GET /api/v1/markets/{marketId} - Get market by ID
     */
    public Mono<ServerResponse> getMarketById(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return marketService.getMarketById(marketId)
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(market);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * GET /api/v1/markets/slug/{slug} - Get market by slug
     */
    public Mono<ServerResponse> getMarketBySlug(ServerRequest request) {
        String slug = request.pathVariable("slug");

        return marketService.getMarketBySlug(slug)
                .flatMap(market -> {
                    ApiResponse<MarketResponse> response = ApiResponse.success(market);
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * GET /api/v1/markets - Get all markets
     */
    public Mono<ServerResponse> getAllMarkets(ServerRequest request) {
        return marketService.getAllMarkets()
                .collectList()
                .flatMap(markets -> {
                    ApiResponse<java.util.List<MarketResponse>> response = ApiResponse.success(markets);
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * GET /api/v1/markets?category={categoryId} - Get markets by category
     */
    public Mono<ServerResponse> getMarketsByCategory(ServerRequest request) {
        return request.queryParam("category")
                .map(categoryIdStr -> {
                    try {
                        UUID categoryId = UUID.fromString(categoryIdStr);
                        return marketService.getMarketsByCategory(categoryId)
                                .collectList()
                                .flatMap(markets -> {
                                    ApiResponse<java.util.List<MarketResponse>> response = ApiResponse.success(markets);
                                    return ServerResponse.ok().bodyValue(response);
                                })
                                .onErrorResume(this::handleError);
                    } catch (IllegalArgumentException e) {
                        return handleError(new IllegalArgumentException("Invalid category ID format"));
                    }
                })
                .orElseGet(() -> getAllMarkets(request));
    }

    /**
     * GET /api/v1/markets?status={status} - Get markets by status
     */
    public Mono<ServerResponse> getMarketsByStatus(ServerRequest request) {
        return request.queryParam("status")
                .map(status ->
                    marketService.getMarketsByStatus(status)
                            .collectList()
                            .flatMap(markets -> {
                                ApiResponse<java.util.List<MarketResponse>> response = ApiResponse.success(markets);
                                return ServerResponse.ok().bodyValue(response);
                            })
                            .onErrorResume(this::handleError)
                )
                .orElseGet(() -> getAllMarkets(request));
    }

    /**
     * GET /api/v1/markets/featured - Get featured markets
     */
    public Mono<ServerResponse> getFeaturedMarkets(ServerRequest request) {
        return marketService.getFeaturedMarkets()
                .collectList()
                .flatMap(markets -> {
                    ApiResponse<java.util.List<MarketResponse>> response = ApiResponse.success(markets);
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * GET /api/v1/markets/trending - Get trending markets
     */
    public Mono<ServerResponse> getTrendingMarkets(ServerRequest request) {
        return marketService.getTrendingMarkets()
                .collectList()
                .flatMap(markets -> {
                    ApiResponse<java.util.List<MarketResponse>> response = ApiResponse.success(markets);
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * PUT /api/v1/markets/{marketId} - Update market
     */
    public Mono<ServerResponse> updateMarket(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");
        UUID updatedBy = UUID.randomUUID(); // TODO: Extract from auth context

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return request.bodyToMono(UpdateMarketRequest.class)
                    .flatMap(req -> marketService.updateMarket(marketId, req, updatedBy))
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(
                                ResponseCode.MARKET_UPDATED,
                                market
                        );
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * POST /api/v1/markets/{marketId}/resolve - Resolve market
     */
    public Mono<ServerResponse> resolveMarket(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");
        UUID resolvedBy = UUID.randomUUID(); // TODO: Extract from auth context

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return request.bodyToMono(ResolveMarketRequest.class)
                    .flatMap(req -> marketService.resolveMarket(marketId, req, resolvedBy))
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(
                                ResponseCode.MARKET_RESOLVED,
                                market
                        );
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * POST /api/v1/markets/{marketId}/close - Close market
     */
    public Mono<ServerResponse> closeMarket(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");
        UUID closedBy = UUID.randomUUID(); // TODO: Extract from auth context

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return marketService.closeMarket(marketId, closedBy)
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(
                                ResponseCode.MARKET_CLOSED,
                                market
                        );
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * POST /api/v1/markets/{marketId}/suspend - Suspend market
     */
    public Mono<ServerResponse> suspendMarket(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");
        UUID suspendedBy = UUID.randomUUID(); // TODO: Extract from auth context

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return marketService.suspendMarket(marketId, "Admin suspended", suspendedBy)
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(market);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * POST /api/v1/markets/{marketId}/reopen - Reopen market
     */
    public Mono<ServerResponse> reopenMarket(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");
        UUID reopenedBy = UUID.randomUUID(); // TODO: Extract from auth context

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return marketService.reopenMarket(marketId, reopenedBy)
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(market);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * DELETE /api/v1/markets/{marketId} - Cancel market
     */
    public Mono<ServerResponse> cancelMarket(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");
        UUID cancelledBy = UUID.randomUUID(); // TODO: Extract from auth context

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return marketService.cancelMarket(marketId, "Admin cancelled", cancelledBy)
                    .flatMap(market -> {
                        ApiResponse<MarketResponse> response = ApiResponse.success(market);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * GET /api/v1/markets/{marketId}/outcomes - Get market outcomes
     */
    public Mono<ServerResponse> getMarketOutcomes(ServerRequest request) {
        String marketIdStr = request.pathVariable("marketId");

        try {
            UUID marketId = UUID.fromString(marketIdStr);

            return marketService.getMarketOutcomes(marketId)
                    .collectList()
                    .flatMap(outcomes -> {
                        ApiResponse<java.util.List<com.oregonMarkets.domain.market.dto.response.OutcomeResponse>> response =
                                ApiResponse.success(outcomes);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * GET /api/v1/markets/search?q={query} - Search markets
     */
    public Mono<ServerResponse> searchMarkets(ServerRequest request) {
        return request.queryParam("q")
                .map(query ->
                    marketService.searchMarkets(query)
                            .collectList()
                            .flatMap(markets -> {
                                ApiResponse<java.util.List<MarketResponse>> response = ApiResponse.success(markets);
                                return ServerResponse.ok().bodyValue(response);
                            })
                            .onErrorResume(this::handleError)
                )
                .orElseGet(() ->
                    ServerResponse.badRequest()
                            .bodyValue(ApiResponse.error(
                                    ResponseCode.MISSING_REQUIRED_FIELD,
                                    "Search query parameter 'q' is required"
                            ))
                );
    }

    // ==================== Error Handling ====================

    private Mono<ServerResponse> handleError(Throwable error) {
        log.error("Error handling request", error);

        if (error instanceof com.oregonMarkets.common.exception.BusinessException) {
            com.oregonMarkets.common.exception.BusinessException be =
                    (com.oregonMarkets.common.exception.BusinessException) error;
            ApiResponse<Void> response = ApiResponse.error(be.getResponseCode(), be.getMessage());
            return ServerResponse
                    .status(be.getResponseCode().getHttpStatus())
                    .bodyValue(response);
        }

        if (error instanceof IllegalArgumentException) {
            ApiResponse<Void> response = ApiResponse.error(
                    ResponseCode.INVALID_INPUT,
                    error.getMessage()
            );
            return ServerResponse.badRequest().bodyValue(response);
        }

        ApiResponse<Void> response = ApiResponse.error(
                ResponseCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        return ServerResponse
                .status(500)
                .bodyValue(response);
    }
}
