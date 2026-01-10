package com.oregonmarkets.domain.market.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating market view configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMarketViewConfigRequest {

    private UUID viewTemplateId;
    private String viewConfigOverride;
}
