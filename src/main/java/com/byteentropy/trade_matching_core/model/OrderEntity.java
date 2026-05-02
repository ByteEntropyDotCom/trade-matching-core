package com.byteentropy.trade_matching_core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(columnNames = "clientOrderId")
})
public class OrderEntity {

    @Id
    private String id; // Internal UUID

    @Column(nullable = false, unique = true)
    private String clientOrderId;

    private String accountId;
    private String symbol;
    
    @Enumerated(EnumType.STRING)
    private Side side;

    @Enumerated(EnumType.STRING)
    private OrderType type;

    private long price;
    private long quantity;
    private long remainingQuantity; // Crucial for recovery
    private boolean isCompleted;

    public OrderEntity() {}

    public OrderEntity(Order order, String clientOrderId) {
        this.id = order.id();
        this.clientOrderId = clientOrderId;
        this.accountId = order.accountId();
        this.symbol = order.symbol();
        this.side = order.side();
        this.type = order.type();
        this.price = order.price();
        this.quantity = order.quantity();
        this.remainingQuantity = order.quantity(); // Initially same as quantity
        this.isCompleted = false;
    }

    // Getters
    public String getId() { return id; }
    public String getClientOrderId() { return clientOrderId; }
    public String getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public OrderType getType() { return type; }
    public long getPrice() { return price; }
    public long getQuantity() { return quantity; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public boolean isCompleted() { return isCompleted; }

    // Setters
    public void setRemainingQuantity(long remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}