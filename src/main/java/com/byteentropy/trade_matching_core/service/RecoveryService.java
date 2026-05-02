package com.byteentropy.trade_matching_core.service;

import com.byteentropy.trade_matching_core.model.Order;
import com.byteentropy.trade_matching_core.model.OrderEntity;
import com.byteentropy.trade_matching_core.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecoveryService {
    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);
    private final OrderRepository orderRepository;
    private final MatchingEngine matchingEngine;

    public RecoveryService(OrderRepository orderRepository, MatchingEngine matchingEngine) {
        this.orderRepository = orderRepository;
        this.matchingEngine = matchingEngine;
    }

    @PostConstruct
    public void recover() {
        log.info("Initiating OrderBook recovery sequence...");

        List<OrderEntity> activeOrders = orderRepository.findAll().stream()
                .filter(o -> !o.isCompleted())
                .toList();

        log.info("Found {} active orders to restore.", activeOrders.size());

        for (OrderEntity entity : activeOrders) {
            Order order = new Order(
                    entity.getId(),
                    entity.getAccountId(),
                    entity.getSymbol(),
                    entity.getSide(),
                    entity.getType(),
                    entity.getPrice(),
                    entity.getRemainingQuantity(), // Error fixed: now exists in OrderEntity
                    System.nanoTime()
            );
            matchingEngine.rebuildOrder(order);
        }

        log.info("Recovery sequence finalized.");
    }
}