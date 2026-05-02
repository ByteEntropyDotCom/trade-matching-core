package com.byteentropy.trade_matching_core.service;

import com.byteentropy.trade_matching_core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    private MatchingEngine matchingEngine;

    @Mock
    private TradePersistenceService persistenceService;

    @Mock
    private SimpMessagingTemplate messagingTemplate; // Added mock for WebSocket template

    // Scale factor for 8 decimal places (1.00000000)
    private static final long SCALE = 100_000_000L;

    @BeforeEach
    void setUp() {
        // Updated constructor to include messagingTemplate
        matchingEngine = new MatchingEngine(persistenceService, messagingTemplate);
    }

    @Test
    void shouldMatchOrdersAndTriggerPersistenceAndBroadcast() throws Exception {
        // GIVEN: A resting Sell order and a matching Buy order
        long price = 50000 * SCALE;
        long qty = 1 * SCALE;
        String symbol = "BTCUSD";

        Order sellOrder = new Order("seller1", symbol, Side.SELL, OrderType.LIMIT, price, qty);
        Order buyOrder = new Order("buyer1", symbol, Side.BUY, OrderType.LIMIT, price, qty);

        // WHEN: Submit maker first, then taker.
        matchingEngine.submitOrder(sellOrder).get(2, TimeUnit.SECONDS);
        CompletableFuture<List<Trade>> futureTrades = matchingEngine.submitOrder(buyOrder);
        
        List<Trade> trades = futureTrades.get(2, TimeUnit.SECONDS);

        // THEN: Verify the match
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).price()).isEqualTo(price);

        // AND: Verify persistence was triggered
        verify(persistenceService, timeout(1000).times(1)).persistTrades(anyList());

        // AND: Verify WebSocket broadcast was triggered (Push Model check)
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/trades"), anyList());
    }

    @Test
    void shouldExecuteSymbolsOnDifferentThreads() throws Exception {
        // GIVEN: Orders for different symbols to verify sharding
        Order btcOrder = new Order("u1", "BTCUSD", Side.BUY, OrderType.LIMIT, 100L * SCALE, 1L * SCALE);
        Order ethOrder = new Order("u2", "ETHUSD", Side.BUY, OrderType.LIMIT, 100L * SCALE, 1L * SCALE);

        // WHEN: Submit both
        CompletableFuture<List<Trade>> btcFuture = matchingEngine.submitOrder(btcOrder);
        CompletableFuture<List<Trade>> ethFuture = matchingEngine.submitOrder(ethOrder);

        // THEN: Both should complete (resting in their respective books)
        assertThat(btcFuture.get(2, TimeUnit.SECONDS)).isEmpty();
        assertThat(ethFuture.get(2, TimeUnit.SECONDS)).isEmpty();
    }

    @Test
    void shouldHandleCancellations() throws Exception {
        String symbol = "SOLUSD";
        Order order = new Order("u1", symbol, Side.BUY, OrderType.LIMIT, 100L * SCALE, 1L * SCALE);
        matchingEngine.submitOrder(order).get();

        // WHEN: Cancel the order via the matching thread
        matchingEngine.cancelOrder(symbol, order.id()).get(1, TimeUnit.SECONDS);

        // THEN: A crossing order should no longer find it
        Order sellOrder = new Order("u2", symbol, Side.SELL, OrderType.LIMIT, 100L * SCALE, 1L * SCALE);
        List<Trade> trades = matchingEngine.submitOrder(sellOrder).get();
        
        assertThat(trades).isEmpty();
    }

    @Test
    void shouldRebuildOrderBookWithoutPersistingOrBroadcasting() throws Exception {
        String symbol = "RECO_TEST";
        Order recoveryOrder = new Order("recovered", symbol, Side.BUY, OrderType.LIMIT, 1000L * SCALE, 500L * SCALE);

        // WHEN: Rebuilding from DB state
        matchingEngine.rebuildOrder(recoveryOrder);
        
        // Give the sharded executor a tiny moment to process the recovery task
        Thread.sleep(150);

        // THEN: Match against the recovered order
        Order taker = new Order("taker", symbol, Side.SELL, OrderType.LIMIT, 1000L * SCALE, 500L * SCALE);
        List<Trade> trades = matchingEngine.submitOrder(taker).get(2, TimeUnit.SECONDS);

        assertThat(trades).hasSize(1);
        
        // Verify broadcast occurred for the match, but NOT during recovery
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/trades"), anyList());
        verify(persistenceService, timeout(1000).times(1)).persistTrades(anyList());
    }
}