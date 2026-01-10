package com.oregonmarkets.domain.market.handler;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.repository.ViewTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Functional handler for view template API endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewTemplateHandler {

    private final ViewTemplateRepository viewTemplateRepository;

    /**
     * GET /api/v1/admin/view-templates - Get all view templates
     */
    public Mono<ServerResponse> getAllViewTemplates(ServerRequest request) {
        return viewTemplateRepository.findAll()
                .collectList()
                .flatMap(templates -> ServerResponse.ok().bodyValue(ApiResponse.success(templates)))
                .onErrorResume(error -> {
                    log.error("Error fetching view templates", error);
                    return ServerResponse.status(500)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to fetch view templates"));
                });
    }
}
