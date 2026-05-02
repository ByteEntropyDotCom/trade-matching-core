package com.byteentropy.trade_matching_core.model;

public record Trade(
    String symbol,       
    String buyOrderId,
    String sellOrderId,
    long price,    // Fixed-point
    long quantity, // Fixed-point
    long timestamp
) {}