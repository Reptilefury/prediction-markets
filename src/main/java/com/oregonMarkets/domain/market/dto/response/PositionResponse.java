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
 * Response DTO for position data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PositionResponse {

    private UUID userId;
    private UUID marketId;
    private String marketTitle;
    private String marketStatus;
    private UUID outcomeId;
    private String outcomeName;

    // Position details
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal lockedQuantity;

    private Double averageEntryPrice; // Converted from E4
    private Long averageEntryPriceE4;

    private BigDecimal totalCost;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;

    // Current market data
    private Double currentPrice;
    private Long currentPriceE4;
    private BigDecimal currentValue;

    // Statistics
    private Long totalTrades;
    private BigDecimal totalBought;
    private BigDecimal totalSold;
    private BigDecimal totalFeesPaid;

    // Resolution
    private Boolean isWinner;
    private BigDecimal payoutAmount;
    private String payoutStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant paidAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant firstTradeAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant lastTradeAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant updatedAt;

    private Long version;
}
