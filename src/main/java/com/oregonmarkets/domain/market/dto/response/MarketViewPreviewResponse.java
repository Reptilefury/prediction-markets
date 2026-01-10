package com.oregonmarkets.domain.market.dto.response;

import com.oregonmarkets.domain.market.model.ViewTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for market view preview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketViewPreviewResponse {

    private MarketResponse market;
    private ViewTemplate viewTemplate;
    private String resolvedViewConfig;
    private ViewTemplate featuredViewTemplate;
    private String resolvedFeaturedViewConfig;
}
