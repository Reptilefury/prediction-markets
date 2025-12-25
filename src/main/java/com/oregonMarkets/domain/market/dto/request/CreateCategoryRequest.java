package com.oregonmarkets.domain.market.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

  @NotBlank(message = "Category name is required")
  private String name;

  @NotBlank(message = "Category slug is required")
  @Pattern(
      regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
      message = "Slug must be lowercase alphanumeric with hyphens only")
  private String slug;

  private String description;

  private String icon; // Icon identifier or emoji

  @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code")
  private String color; // Hex color code for UI

  private Integer displayOrder;

  @Builder.Default private Boolean enabled = true;
}
