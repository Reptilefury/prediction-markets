package com.oregonmarkets.domain.market.handler;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.model.MarketTypeEntity;
import com.oregonmarkets.domain.market.repository.MarketTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketTypeHandler {

    private final MarketTypeRepository marketTypeRepository;

    public Mono<ServerResponse> getAllMarketTypes(ServerRequest request) {
        return marketTypeRepository.findAll()
                .collectList()
                .flatMap(types -> ServerResponse.ok().bodyValue(
                        ApiResponse.success(ResponseCode.SUCCESS, types)))
                .onErrorResume(e -> {
                    log.error("Error fetching market types", e);
                    return ServerResponse.ok().bodyValue(
                            ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to fetch market types"));
                });
    }
}
