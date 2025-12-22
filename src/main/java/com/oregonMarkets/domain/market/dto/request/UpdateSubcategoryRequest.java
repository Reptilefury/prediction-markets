package com.oregonMarkets.domain.market.dto.request;

import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing subcategory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubcategoryRequest {

  private UUID parentSubcategoryId;

  private String name;

  @Pattern(
      regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
      message = "Slug must be lowercase alphanumeric with hyphens only")
  private String slug;

  private String description;

  private Integer level;

  private String path;

  private Integer displayOrder;

  private Boolean enabled;
}
