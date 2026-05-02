package com.byteentropy.trade_matching_core.dto;

import com.byteentropy.trade_matching_core.model.OrderType;
import com.byteentropy.trade_matching_core.model.Side;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO for incoming order placement.
 * Supports both LIMIT and MARKET orders.
 */
public record OrderRequest(

    @NotBlank(message = "Account ID is required")
    String accountId,

    @NotBlank(message = "Symbol is required (e.g., BTCUSD)")
    String symbol,

    @NotNull(message = "Order Side (BUY/SELL) is required")
    Side side,

    @NotNull(message = "Order Type (LIMIT/MARKET) is required")
    OrderType type,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true) // Allow 0.0 for MARKET orders
    @Digits(integer = 10, fraction = 8)         // Increased precision to 8 for fixed-point
    BigDecimal price,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false) // Quantity must always be > 0
    @Digits(integer = 10, fraction = 8)
    BigDecimal quantity,

    @NotBlank(message = "Client Order ID is required for idempotency")
    String clientOrderId
) {}