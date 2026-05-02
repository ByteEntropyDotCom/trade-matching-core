package com.byteentropy.trade_matching_core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String buyOrderId;

    @Column(nullable = false)
    private String sellOrderId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false)
    private long timestamp;

    // Required by JPA
    public TradeEntity() {}

    public TradeEntity(Trade trade) {
        this.symbol = trade.symbol();
        this.buyOrderId = trade.buyOrderId();
        this.sellOrderId = trade.sellOrderId();
        // Convert back to BigDecimal for JPA storage if using Decimal columns
        this.price = java.math.BigDecimal.valueOf(trade.price(), 8);
        this.quantity = java.math.BigDecimal.valueOf(trade.quantity(), 8);
        this.timestamp = trade.timestamp();
    }

    // Getters (setters optional unless needed)

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }
}