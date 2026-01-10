package com.oregonmarkets.domain.market.handler;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.dto.request.UpdateFeaturedMarketConfigRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateMarketViewConfigRequest;
import com.oregonmarkets.domain.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Admin handler for market view configuration endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMarketHandler {

    private final MarketService marketService;

    /**
     * PATCH /api/v1/admin/markets/{marketId}/view-config
     */
    public Mono<ServerResponse> updateMarketViewConfig(ServerRequest request) {
        try {
            UUID marketId = UUID.fromString(request.pathVariable("marketId"));
            UUID updatedBy = UUID.randomUUID(); // TODO: Replace with authenticated user
            return request.bodyToMono(UpdateMarketViewConfigRequest.class)
                    .flatMap(body -> marketService.updateMarketViewConfig(marketId, body, updatedBy))
                    .flatMap(response -> ServerResponse.ok().bodyValue(ApiResponse.success(response)))
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException error) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * PATCH /api/v1/admin/markets/{marketId}/featured-config
     */
    public Mono<ServerResponse> updateFeaturedMarketConfig(ServerRequest request) {
        try {
            UUID marketId = UUID.fromString(request.pathVariable("marketId"));
            UUID updatedBy = UUID.randomUUID(); // TODO: Replace with authenticated user
            return request.bodyToMono(UpdateFeaturedMarketConfigRequest.class)
                    .flatMap(body -> marketService.updateFeaturedMarketConfig(marketId, body, updatedBy))
                    .flatMap(response -> ServerResponse.ok().bodyValue(ApiResponse.success(response)))
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException error) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    /**
     * GET /api/v1/admin/markets/{marketId}/view-preview
     */
    public Mono<ServerResponse> getMarketViewPreview(ServerRequest request) {
        try {
            UUID marketId = UUID.fromString(request.pathVariable("marketId"));
            return marketService.getMarketViewPreview(marketId)
                    .flatMap(response -> ServerResponse.ok().bodyValue(ApiResponse.success(response)))
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException error) {
            return handleError(new IllegalArgumentException("Invalid market ID format"));
        }
    }

    private Mono<ServerResponse> handleError(Throwable error) {
        log.error("Error handling admin market request", error);

        if (error instanceof com.oregonmarkets.common.exception.BusinessException) {
            com.oregonmarkets.common.exception.BusinessException be =
                    (com.oregonmarkets.common.exception.BusinessException) error;
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
