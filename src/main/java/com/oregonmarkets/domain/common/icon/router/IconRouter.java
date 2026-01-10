package com.oregonmarkets.domain.common.icon.router;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.common.icon.dto.IconSearchResponse;
import com.oregonmarkets.domain.common.icon.service.IconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Router configuration for icon endpoints
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class IconRouter {

  private final IconService iconService;

  @Bean
  public RouterFunction<ServerResponse> iconRoutes() {
    return RouterFunctions.route()
        .GET("/api/icons/search", this::searchIcons)
        .GET("/api/icons/{iconName}/svg", this::getIconSvg)
        .build();
  }

  /**
   * Search icons by query
   * GET /api/icons/search?query=sports&limit=24&color=00c896
   */
  private Mono<ServerResponse> searchIcons(ServerRequest request) {
    String query = request.queryParam("query").orElse("");
    Integer limit = request.queryParam("limit").map(Integer::parseInt).orElse(24);
    String color = request.queryParam("color").orElse(null);

    log.info("Icon search request - query: {}, limit: {}, color: {}", query, limit, color);

    if (query.isEmpty()) {
      return ServerResponse.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              ApiResponse.<List<IconSearchResponse>>error(
                  ResponseCode.VALIDATION_ERROR, "Query parameter is required"));
    }

    return iconService
        .searchIcons(query, limit, color)
        .flatMap(
            icons ->
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(icons)))
        .onErrorResume(
            error -> {
              log.error("Error searching icons: {}", error.getMessage());
              return ServerResponse.ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue(
                      ApiResponse.<List<IconSearchResponse>>error(
                          ResponseCode.INTERNAL_SERVER_ERROR,
                          "Failed to search icons: " + error.getMessage()));
            });
  }

  /**
   * Get SVG content for an icon
   * GET /api/icons/{iconName}/svg?color=00c896
   */
  private Mono<ServerResponse> getIconSvg(ServerRequest request) {
    String iconName = request.pathVariable("iconName");
    String color = request.queryParam("color").orElse(null);

    log.info("Icon SVG request - iconName: {}, color: {}", iconName, color);

    return iconService
        .getIconSvg(iconName, color)
        .flatMap(
            svg ->
                ServerResponse.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .bodyValue(svg))
        .onErrorResume(
            error -> {
              log.error("Error fetching icon SVG: {}", error.getMessage());
              return ServerResponse.ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue(
                      ApiResponse.<String>error(
                          ResponseCode.INTERNAL_SERVER_ERROR,
                          "Failed to fetch icon: " + error.getMessage()));
            });
  }
}
