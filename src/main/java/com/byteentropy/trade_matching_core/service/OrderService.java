package com.byteentropy.trade_matching_core.service;

import com.byteentropy.trade_matching_core.dto.OrderRequest;
import com.byteentropy.trade_matching_core.model.*;
import com.byteentropy.trade_matching_core.repository.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    private final MatchingEngine matchingEngine;
    private final OrderRepository orderRepository;

    private static final int SCALE = 8;
    private static final long TICK_SIZE = 1_000_000L; 

    public OrderService(MatchingEngine matchingEngine, OrderRepository orderRepository) {
        this.matchingEngine = matchingEngine;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public CompletableFuture<List<Trade>> processOrder(OrderRequest request) {
        long price = request.price().movePointRight(SCALE).longValue();
        long quantity = request.quantity().movePointRight(SCALE).longValue();

        if (request.type() == OrderType.LIMIT && price % TICK_SIZE != 0) {
            throw new IllegalArgumentException("Invalid price for tick size");
        }

        Order order = new Order(request.accountId(), request.symbol(), request.side(), 
                                request.type(), price, quantity);

        // Idempotency and Persistence for Recovery
        try {
            orderRepository.save(new OrderEntity(order, request.clientOrderId()));
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Order already processed: " + request.clientOrderId());
        }

        return matchingEngine.submitOrder(order);
    }

    public CompletableFuture<Void> cancelOrder(String symbol, String orderId) {
        return matchingEngine.cancelOrder(symbol, orderId);
    }
}