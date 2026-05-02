package com.byteentropy.trade_matching_core.model;

import java.util.*;

public class OrderBook {
    private final String symbol;
    private final TreeMap<Long, Deque<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Long, Deque<Order>> asks = new TreeMap<>();
    private final Map<String, Order> activeOrders = new HashMap<>(); 

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public void cancel(String orderId) {
        Order order = activeOrders.remove(orderId);
        if (order == null) return;
        removeOrderFromBook(order);
    }

    public List<Trade> match(Order newOrder) {
        if (newOrder.quantity() <= 0) throw new IllegalArgumentException("Quantity must be positive");
        
        if (newOrder.type() == OrderType.POST_ONLY && canMatchImmediately(newOrder)) {
            return Collections.emptyList(); 
        }

        List<Trade> trades = new ArrayList<>();
        long originalQty = newOrder.quantity();

        if (newOrder.side() == Side.BUY) {
            matchOrder(newOrder, asks, trades);
        } else {
            matchOrder(newOrder, bids, trades);
        }

        if (newOrder.type() == OrderType.FOK && trades.stream().mapToLong(Trade::quantity).sum() < originalQty) {
            trades.clear();
            return trades;
        }

        return trades;
    }

    private boolean canMatchImmediately(Order order) {
        if (order.side() == Side.BUY) {
            return !asks.isEmpty() && order.price() >= asks.firstKey();
        } else {
            return !bids.isEmpty() && order.price() <= bids.firstKey();
        }
    }

    private void matchOrder(Order order, TreeMap<Long, Deque<Order>> opposites, List<Trade> trades) {
        long remainingQty = order.quantity();
        
        while (remainingQty > 0 && !opposites.isEmpty()) {
            long bestOppositePrice = opposites.firstKey();
            
            if (order.type() != OrderType.MARKET) {
                boolean canMatch = (order.side() == Side.BUY) ? 
                    order.price() >= bestOppositePrice : 
                    order.price() <= bestOppositePrice;
                if (!canMatch) break;
            }

            Deque<Order> ordersAtPrice = opposites.get(bestOppositePrice);
            
            // SELF-TRADE PREVENTION: If the head of the queue is the same account, 
            // we must stop matching this order to avoid an infinite loop.
            if (!ordersAtPrice.isEmpty() && ordersAtPrice.peekFirst().accountId().equals(order.accountId())) {
                break; 
            }

            while (!ordersAtPrice.isEmpty() && remainingQty > 0) {
                Order oppositeOrder = ordersAtPrice.peekFirst();
                if (oppositeOrder.accountId().equals(order.accountId())) break; 

                long matchQty = Math.min(remainingQty, oppositeOrder.quantity());
                trades.add(new Trade(this.symbol, 
                    order.side() == Side.BUY ? order.id() : oppositeOrder.id(),
                    order.side() == Side.SELL ? order.id() : oppositeOrder.id(),
                    bestOppositePrice, matchQty, System.currentTimeMillis()));

                remainingQty -= matchQty;
                
                if (matchQty == oppositeOrder.quantity()) {
                    activeOrders.remove(ordersAtPrice.removeFirst().id());
                } else {
                    updateRestingOrder(ordersAtPrice, oppositeOrder, matchQty);
                }
            }
            
            if (ordersAtPrice.isEmpty()) {
                opposites.remove(bestOppositePrice);
            } else {
                // If queue is not empty but we exited the inner loop, it was STP.
                // We must break the outer loop to prevent hanging.
                break; 
            }
        }

        if (remainingQty > 0 && (order.type() == OrderType.LIMIT || order.type() == OrderType.POST_ONLY)) {
            addOrderToBook(new Order(order.id(), order.accountId(), order.symbol(), 
                order.side(), order.type(), order.price(), remainingQty, order.timestamp()));
        }
    }

    public void addOrderToBook(Order order) {
        var book = (order.side() == Side.BUY) ? bids : asks;
        book.computeIfAbsent(order.price(), k -> new ArrayDeque<>()).addLast(order);
        activeOrders.put(order.id(), order);
    }

    private void updateRestingOrder(Deque<Order> queue, Order oldOrder, long matchQty) {
        Order updated = new Order(oldOrder.id(), oldOrder.accountId(), oldOrder.symbol(), 
            oldOrder.side(), oldOrder.type(), oldOrder.price(), oldOrder.quantity() - matchQty, oldOrder.timestamp());
        queue.removeFirst();
        queue.addFirst(updated);
        activeOrders.put(updated.id(), updated);
    }

    private void removeOrderFromBook(Order order) {
        var book = (order.side() == Side.BUY) ? bids : asks;
        Deque<Order> queue = book.get(order.price());
        if (queue != null) {
            queue.removeIf(o -> o.id().equals(order.id()));
            if (queue.isEmpty()) book.remove(order.price());
        }
    }
}