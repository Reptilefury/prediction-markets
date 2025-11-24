package com.oregonMarkets.security;

import com.oregonMarkets.common.exception.MagicAuthException;
import com.oregonMarkets.dto.ErrorType;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class MagicTokenFilter implements WebFilter {

    private final MagicDIDValidator magicValidator;
    private final ErrorResponseBuilder errorResponseBuilder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        log.info("MagicTokenFilter: Processing {} request for path: {}", method, path);

        if (path.startsWith("/api/") || path.startsWith("/prediction-markets/api/")) {
            log.info("MagicTokenFilter: API path detected, validating Magic token for path: {}", path);
            return validateAndProceed(exchange, chain);
        }

        log.info("MagicTokenFilter: Non-API path, skipping Magic token validation for path: {}", path);
        return chain.filter(exchange);
    }

    private Mono<Void> validateAndProceed(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractToken(exchange);
        String path = exchange.getRequest().getPath().value();
        log.info("MagicTokenFilter: Extracted token for path {}: {}", path, token != null ? "present" : "missing");

        if (token == null) {
            log.warn("MagicTokenFilter: Missing or invalid Authorization header for path: {}", path);
            return sendErrorResponse(exchange, "Missing or invalid Authorization header");
        }

        log.info("MagicTokenFilter: Validating Magic token for path: {}", path);
        return magicValidator.validateDIDToken(token)
            .flatMap(userInfo -> {
                log.info("MagicTokenFilter: Magic DID token validation successful for user: {} on path: {}", userInfo.getEmail(), path);
                exchange.getAttributes().put("magicUser", userInfo);
                exchange.getAttributes().put("magicToken", token);
                return chain.filter(exchange);
            })
            .onErrorResume(e -> {
                log.error("MagicTokenFilter: Magic DID token validation failed for path {}: {}", path, e.getMessage(), e);
                return sendErrorResponse(exchange, getErrorMessage(e));
            });
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        log.debug("Authorization header present: {}", authHeader != null);
        if (authHeader != null) {
            log.debug("Authorization header length: {}, First 50 chars: {}",
                authHeader.length(),
                authHeader.substring(0, Math.min(50, authHeader.length())));
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        log.debug("Extracted token length: {}, First 50 chars: {}",
            token.length(),
            token.substring(0, Math.min(50, token.length())));
        return token;
    }

    private String getErrorMessage(Throwable e) {
        return e instanceof MagicAuthException ? e.getMessage() : "Magic token validation failed";
    }

    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, String message) {
        log.warn("Sending error response: {} for path: {}", message, exchange.getRequest().getPath().value());
        exchange.getResponse().setStatusCode(ErrorType.MAGIC_AUTH_FAILED.getHttpStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = errorResponseBuilder.buildErrorResponse(ErrorType.MAGIC_AUTH_FAILED, message);

        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
