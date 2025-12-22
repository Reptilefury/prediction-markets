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
 * Response DTO for trade data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradeResponse {

    private UUID tradeId;
    private UUID marketId;
    private UUID outcomeId;
    private String outcomeName;

    // Buyer information
    private UUID buyerOrderId;
    private UUID buyerUserId;
    private String buyerUsername;

    // Seller information
    private UUID sellerOrderId;
    private UUID sellerUserId;
    private String sellerUsername;

    // Trade details
    private Double price; // Converted from E4
    private Long priceE4;
    private BigDecimal quantity;
    private BigDecimal totalValue;

    // Fees
    private BigDecimal buyerFee;
    private BigDecimal sellerFee;
    private BigDecimal platformFee;
    private BigDecimal creatorFee;

    // Maker/Taker
    private String makerSide;
    private String takerSide;
    private UUID makerUserId;
    private UUID takerUserId;

    // Settlement
    private String settlementStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant settledAt;

    private String settlementTransactionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant executedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;
}
