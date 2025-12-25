package com.oregonmarkets.domain.market.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for resolving a market
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveMarketRequest {

    @NotNull(message = "Winning outcome ID is required")
    private UUID winningOutcomeId;

    @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
    private String resolutionNotes;
}
