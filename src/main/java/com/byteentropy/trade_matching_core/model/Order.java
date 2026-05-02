package com.byteentropy.trade_matching_core.model;
import java.util.UUID;

public record Order(
    String id,
    String accountId,
    String symbol,
    Side side,
    OrderType type,
    long price,     // 0 for Market Orders
    long quantity,
    long timestamp
) {
    public Order(String accountId, String symbol, Side side, OrderType type, long price, long quantity) {
        this(UUID.randomUUID().toString(), accountId, symbol, side, type, price, quantity, System.nanoTime());
    }
}