package com.byteentropy.trade_matching_core.model;

public enum OrderType { 
    LIMIT, 
    MARKET, 
    IOC,        // Immediate or Cancel: Match what you can, cancel the rest
    FOK,        // Fill or Kill: Match the whole thing or nothing
    POST_ONLY   // Must be a Maker order; reject if it would match immediately
}