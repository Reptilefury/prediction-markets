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
 * Order Book Entry - Real-time order book data
 * Table: order_book
 * TTL: 7 days (automatically deleted)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_book")
public class OrderBookEntry {

    @PrimaryKeyColumn(name = "market_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID marketId;

    @PrimaryKeyColumn(name = "outcome_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private UUID outcomeId;

    @PrimaryKeyColumn(name = "side", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    private String side; // BUY or SELL

    @PrimaryKeyColumn(name = "price_e4", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
    private Long priceE4;

    @PrimaryKeyColumn(name = "timestamp", ordinal = 4, type = PrimaryKeyType.CLUSTERED)
    private Instant timestamp;

    @PrimaryKeyColumn(name = "order_id", ordinal = 5, type = PrimaryKeyType.CLUSTERED)
    private UUID orderId;

    @Column("user_id")
    private UUID userId;

    @Column("quantity")
    private BigDecimal quantity;

    @Column("filled_quantity")
    private BigDecimal filledQuantity;

    @Column("remaining_quantity")
    private BigDecimal remainingQuantity;

    @Column("created_at")
    private Instant createdAt;
}
