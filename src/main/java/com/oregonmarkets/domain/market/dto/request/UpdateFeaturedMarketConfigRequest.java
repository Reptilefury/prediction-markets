package com.oregonmarkets.domain.market.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating featured market configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeaturedMarketConfigRequest {

    private Boolean featured;
    private UUID featuredViewTemplateId;
    private String featuredViewConfigOverride;
    private Integer featuredRank;
}
