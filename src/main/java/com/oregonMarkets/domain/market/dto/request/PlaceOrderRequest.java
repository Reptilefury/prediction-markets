package com.oregonmarkets.domain.market.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for placing an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Market ID is required")
    private UUID marketId;

    @NotNull(message = "Outcome ID is required")
    private UUID outcomeId;

    @NotNull(message = "Order side is required")
    @Pattern(regexp = "BUY|SELL", message = "Side must be BUY or SELL")
    private String side;

    @NotNull(message = "Order type is required")
    @Pattern(regexp = "LIMIT|MARKET", message = "Order type must be LIMIT or MARKET")
    private String orderType;

    @NotNull(message = "Price is required for limit orders")
    @Min(value = 0, message = "Price must be non-negative")
    @Max(value = 10000, message = "Price cannot exceed 10000 (100.00%)")
    private Long priceE4; // Price in basis points

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be at least 0.01")
    private BigDecimal quantity;

    @Pattern(regexp = "GTC|IOC|FOK|DAY", message = "Invalid time in force")
    private String timeInForce; // Good Till Cancel, Immediate Or Cancel, Fill Or Kill, Day

    private String clientOrderId; // Optional user-provided order ID
}
