package com.byteentropy.trade_matching_core.service;

import com.byteentropy.trade_matching_core.model.Trade;
import com.byteentropy.trade_matching_core.model.TradeEntity;
import com.byteentropy.trade_matching_core.repository.TradeRepository;
import com.byteentropy.trade_matching_core.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class TradePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TradePersistenceService.class);
    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;

    public TradePersistenceService(TradeRepository tradeRepository, OrderRepository orderRepository) {
        this.tradeRepository = tradeRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void persistTrades(List<Trade> trades) {
        if (trades.isEmpty()) return;

        try {
            // 1. Batch save all Trade entities
            List<TradeEntity> entities = trades.stream().map(TradeEntity::new).toList();
            tradeRepository.saveAll(entities);

            // 2. Efficiently update order quantities
            for (Trade trade : trades) {
                applyTradeToOrder(trade.buyOrderId(), trade.quantity());
                applyTradeToOrder(trade.sellOrderId(), trade.quantity());
            }

            log.info("Successfully persisted {} trades.", entities.size());

        } catch (Exception e) {
            log.error("CRITICAL: Trade persistence failed: {}", e.getMessage());
            throw e; 
        }
    }

    private void applyTradeToOrder(String orderId, long matchedQty) {
        orderRepository.findById(orderId).ifPresent(order -> {
            long newRemaining = Math.max(0, order.getRemainingQuantity() - matchedQty);
            order.setRemainingQuantity(newRemaining);
            if (newRemaining <= 0) {
                order.setCompleted(true);
            }
            // Note: In a @Transactional method, explicit .save() isn't strictly required 
            // if the entity is managed, but we keep it for clarity with some JPA configs.
            orderRepository.save(order);
        });
    }
}