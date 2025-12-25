package com.oregonmarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outcome entity for markets
 * Table: outcomes
 * Each market has multiple outcomes (e.g., Yes/No for binary, or multiple choices)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("outcomes")
public class Outcome {

    @PrimaryKeyColumn(name = "market_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID marketId;

    @PrimaryKeyColumn(name = "outcome_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID outcomeId;

    @Column("name")
    private String name; // e.g., "Yes", "No", "Team A", "Candidate X"

    @Column("description")
    private String description;

    @Column("display_order")
    private Integer displayOrder;

    @Column("color")
    private String color; // Hex color for UI

    @Column("icon")
    private String icon;

    // Price and volume data
    @Column("current_price_e4")
    private Long currentPriceE4; // Price in basis points (e.g., 5000 = 50.00%)

    @Column("last_price_e4")
    private Long lastPriceE4;

    @Column("best_bid_e4")
    private Long bestBidE4;

    @Column("best_ask_e4")
    private Long bestAskE4;

    @Column("total_volume")
    private BigDecimal totalVolume;

    @Column("volume_24h")
    private BigDecimal volume24h;

    @Column("open_interest")
    private BigDecimal openInterest; // Total outstanding shares

    @Column("total_liquidity")
    private BigDecimal totalLiquidity;

    // Resolution
    @Column("is_winner")
    private Boolean isWinner;

    @Column("payout_amount")
    private BigDecimal payoutAmount;

    // Metadata
    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
