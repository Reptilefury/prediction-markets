package com.oregonmarkets.domain.common.icon.service;

import com.oregonmarkets.domain.common.icon.dto.IconSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Iconify API
 */
@Service
@Slf4j
public class IconService {

  private static final String ICONIFY_API_BASE = "https://api.iconify.design";
  private static final String DEFAULT_PREFIX = "mdi"; // Material Design Icons
  private final WebClient webClient;

  public IconService(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl(ICONIFY_API_BASE).build();
  }

  /**
   * Search for icons by query
   */
  public Mono<List<IconSearchResponse>> searchIcons(String query, Integer limit, String color) {
    int searchLimit = limit != null ? limit : 24;

    log.debug("Searching icons with query: {}, limit: {}, color: {}", query, searchLimit, color);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/search")
                    .queryParam("query", query)
                    .queryParam("limit", searchLimit)
                    .queryParam("prefix", DEFAULT_PREFIX)
                    .build())
        .retrieve()
        .bodyToMono(Map.class)
        .flatMapMany(
            response -> {
              List<String> icons = (List<String>) response.get("icons");
              if (icons == null || icons.isEmpty()) {
                return Flux.empty();
              }
              return Flux.fromIterable(icons);
            })
        .flatMap(iconName -> 
            getIconSvg(iconName, color)
                .map(svg -> mapToIconResponse(iconName, svg))
                .onErrorResume(e -> {
                  log.warn("Failed to fetch SVG for {}: {}", iconName, e.getMessage());
                  return Mono.just(mapToIconResponse(iconName, null));
                })
        )
        .collectList()
        .doOnSuccess(
            icons -> log.debug("Found {} icons for query: {}", icons.size(), query))
        .doOnError(error -> log.error("Error searching icons: {}", error.getMessage()));
  }

  /**
   * Get SVG content for a specific icon
   */
  public Mono<String> getIconSvg(String iconName, String color) {
    log.debug("Fetching SVG for icon: {}, color: {}", iconName, color);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              var builder = uriBuilder.path("/{iconName}.svg");
              if (color != null && !color.isEmpty()) {
                // Remove # from color if present, Iconify expects just hex value
                String hexColor = color.replace("#", "");
                builder.queryParam("color", hexColor);
              }
              return builder.build(iconName);
            })
        .retrieve()
        .bodyToMono(String.class)
        .map(svg -> {
          // Fix Iconify's color format - add # to fill attribute
          if (color != null && !color.isEmpty()) {
            String hexColor = color.replace("#", "");
            svg = svg.replace("fill=\"" + hexColor + "\"", "fill=\"#" + hexColor + "\"");
          }
          return svg;
        })
        .doOnSuccess(svg -> log.debug("Successfully fetched SVG for: {}", iconName))
        .doOnError(error -> log.error("Error fetching SVG for {}: {}", iconName, error.getMessage()));
  }

  /**
   * Map icon string to response DTO
   */
  private IconSearchResponse mapToIconResponse(String fullIconName, String svg) {
    String[] parts = fullIconName.split(":");
    String prefix = parts.length > 1 ? parts[0] : DEFAULT_PREFIX;
    String iconName = parts.length > 1 ? parts[1] : parts[0];

    String svgBase64 = null;
    if (svg != null) {
      svgBase64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes());
    }

    return IconSearchResponse.builder()
        .name(fullIconName)
        .prefix(prefix)
        .iconName(iconName)
        .displayName(formatDisplayName(iconName))
        .svgUrl(ICONIFY_API_BASE + "/" + fullIconName + ".svg")
        .svgBase64(svgBase64)
        .build();
  }

  /**
   * Format icon name for display (e.g., "basketball-ball" -> "Basketball Ball")
   */
  private String formatDisplayName(String iconName) {
    StringBuilder result = new StringBuilder();
    String[] words = iconName.replace("-", " ").replace("_", " ").split(" ");
    
    for (String word : words) {
      if (!word.isEmpty()) {
        result.append(Character.toUpperCase(word.charAt(0)))
              .append(word.substring(1).toLowerCase())
              .append(" ");
      }
    }
    
    return result.toString().trim();
  }
}
