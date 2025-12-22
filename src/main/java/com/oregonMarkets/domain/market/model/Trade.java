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
import java.util.UUID;

/**
 * Trade entity - Represents a matched/executed trade
 * Table: trades_by_market
 * Created when two orders match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trades_by_market")
public class Trade {

    @PrimaryKeyColumn(name = "market_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID marketId;

    @PrimaryKeyColumn(name = "executed_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Instant executedAt;

    @PrimaryKeyColumn(name = "trade_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private UUID tradeId;

    // Outcome
    @Column("outcome_id")
    private UUID outcomeId;

    @Column("outcome_name")
    private String outcomeName; // Denormalized

    // Buyer Information
    @Column("buyer_order_id")
    private UUID buyerOrderId;

    @Column("buyer_user_id")
    private UUID buyerUserId;

    @Column("buyer_username")
    private String buyerUsername; // Denormalized

    // Seller Information
    @Column("seller_order_id")
    private UUID sellerOrderId;

    @Column("seller_user_id")
    private UUID sellerUserId;

    @Column("seller_username")
    private String sellerUsername; // Denormalized

    // Trade Details
    @Column("price_e4")
    private Long priceE4; // Execution price in basis points

    @Column("quantity")
    private BigDecimal quantity;

    @Column("total_value")
    private BigDecimal totalValue;

    // Fees
    @Column("buyer_fee")
    private BigDecimal buyerFee;

    @Column("seller_fee")
    private BigDecimal sellerFee;

    @Column("platform_fee")
    private BigDecimal platformFee;

    @Column("creator_fee")
    private BigDecimal creatorFee;

    // Maker/Taker
    @Column("maker_side")
    private String makerSide; // BUY or SELL

    @Column("taker_side")
    private String takerSide; // BUY or SELL

    @Column("maker_user_id")
    private UUID makerUserId;

    @Column("taker_user_id")
    private UUID takerUserId;

    // Settlement
    @Column("settlement_status")
    private String settlementStatus; // PENDING, SETTLED, FAILED

    @Column("settled_at")
    private Instant settledAt;

    @Column("settlement_transaction_id")
    private String settlementTransactionId;

    // Metadata
    @Column("created_at")
    private Instant createdAt;
}
