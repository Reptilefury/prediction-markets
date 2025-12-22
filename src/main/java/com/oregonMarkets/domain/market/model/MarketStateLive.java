package com.oregonMarkets.domain.market.model;

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
import java.util.Map;
import java.util.UUID;

/**
 * Market State Live - Real-time market state snapshots
 * Table: market_state_live
 * Updated frequently for analytics and monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("market_state_live")
public class MarketStateLive {

    @PrimaryKeyColumn(name = "market_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID marketId;

    @PrimaryKeyColumn(name = "timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Instant timestamp;

    // Market Status
    @Column("status")
    private String status;

    // Aggregate Metrics
    @Column("total_volume")
    private BigDecimal totalVolume;

    @Column("volume_1h")
    private BigDecimal volume1h;

    @Column("volume_24h")
    private BigDecimal volume24h;

    @Column("total_liquidity")
    private BigDecimal totalLiquidity;

    @Column("open_interest")
    private BigDecimal openInterest;

    @Column("total_traders")
    private Long totalTraders;

    @Column("active_traders_24h")
    private Long activeTraders24h;

    @Column("total_orders")
    private Long totalOrders;

    @Column("open_orders")
    private Long openOrders;

    // Price Data by Outcome
    @Column("outcome_prices")
    private Map<UUID, Long> outcomePrices; // outcome_id -> price_e4

    @Column("outcome_volumes")
    private Map<UUID, BigDecimal> outcomeVolumes;

    @Column("outcome_best_bids")
    private Map<UUID, Long> outcomeBestBids;

    @Column("outcome_best_asks")
    private Map<UUID, Long> outcomeBestAsks;

    // Spread and Depth
    @Column("average_spread_e4")
    private Long averageSpreadE4;

    @Column("order_book_depth")
    private BigDecimal orderBookDepth;

    // Trading Activity
    @Column("trades_1h")
    private Long trades1h;

    @Column("trades_24h")
    private Long trades24h;

    @Column("last_trade_time")
    private Instant lastTradeTime;

    @Column("last_trade_price_e4")
    private Long lastTradePriceE4;

    // System Health
    @Column("update_latency_ms")
    private Long updateLatencyMs;

    @Column("data_quality_score")
    private Double dataQualityScore;

    @Column("created_at")
    private Instant createdAt;
}
