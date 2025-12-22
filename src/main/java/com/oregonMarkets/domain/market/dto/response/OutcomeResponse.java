package com.oregonMarkets.domain.market.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for outcome data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutcomeResponse {

    private UUID outcomeId;
    private UUID marketId;
    private String name;
    private String description;
    private Integer displayOrder;
    private String color;
    private String icon;

    // Price data
    private Double currentPrice; // Converted from E4 to percentage (e.g., 50.00)
    private Long currentPriceE4;
    private Double lastPrice;
    private Long lastPriceE4;
    private Double bestBid;
    private Long bestBidE4;
    private Double bestAsk;
    private Long bestAskE4;

    // Volume and liquidity
    private BigDecimal totalVolume;
    private BigDecimal volume24h;
    private BigDecimal openInterest;
    private BigDecimal totalLiquidity;

    // Resolution
    private Boolean isWinner;
    private BigDecimal payoutAmount;

    private Boolean enabled;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant updatedAt;
}
