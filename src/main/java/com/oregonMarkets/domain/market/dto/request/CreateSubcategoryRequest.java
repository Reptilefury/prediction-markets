package com.oregonMarkets.domain.market.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new subcategory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubcategoryRequest {

  @NotNull(message = "Category ID is required")
  private UUID categoryId;

  private UUID parentSubcategoryId; // For nested hierarchies

  @NotBlank(message = "Subcategory name is required")
  private String name;

  @NotBlank(message = "Subcategory slug is required")
  @Pattern(
      regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
      message = "Slug must be lowercase alphanumeric with hyphens only")
  private String slug;

  private String description;

  private Integer level; // Hierarchy level (1, 2, 3...)

  private String path; // Full path: "Sports > Basketball > NBA"

  private Integer displayOrder;

  @Builder.Default private Boolean enabled = true;
}
