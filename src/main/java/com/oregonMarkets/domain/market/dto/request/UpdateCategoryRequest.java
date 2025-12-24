package com.oregonMarkets.domain.market.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

  private String name;

  @Pattern(
      regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
      message = "Slug must be lowercase alphanumeric with hyphens only")
  private String slug;

  private String description;

  private String icon;

  @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code")
  private String color;

  private Integer displayOrder;

  private Boolean enabled;
}
