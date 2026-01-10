package com.oregonmarkets.domain.common.icon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for icon search results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IconSearchResponse {

  private String name;        // e.g., "mdi:basketball"
  private String displayName; // e.g., "Basketball"
  private String prefix;      // e.g., "mdi"
  private String iconName;    // e.g., "basketball"
  private String svgUrl;      // URL to fetch SVG
  private String svgBase64;   // Base64 encoded SVG for quick rendering
}
