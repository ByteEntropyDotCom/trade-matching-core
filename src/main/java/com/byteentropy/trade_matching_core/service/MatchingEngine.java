package com.byteentropy.trade_matching_core.service;

import com.byteentropy.trade_matching_core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * The core orchestrator of the trading system.
 * Updated for Push Model: Broadcasts matches via WebSockets in real-time.
 */
@Service
public class MatchingEngine {
    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> engines = new ConcurrentHashMap<>();
    
    // Dedicated pool for database I/O to keep matching threads non-blocking
    private final ExecutorService dbWriterExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    private final TradePersistenceService persistenceService;
    private final SimpMessagingTemplate messagingTemplate; // For WebSocket Broadcaster

    public MatchingEngine(TradePersistenceService persistenceService, SimpMessagingTemplate messagingTemplate) {
        this.persistenceService = persistenceService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Submits an order for matching and broadcasts results to listeners.
     */
    public CompletableFuture<List<Trade>> submitOrder(Order order) {
        String symbol = order.symbol();
        OrderBook book = books.computeIfAbsent(symbol, OrderBook::new);
        ExecutorService executor = engines.computeIfAbsent(symbol, this::createExecutor);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Execute matching logic
                List<Trade> trades = book.match(order);

                if (!trades.isEmpty()) {
                    // 2. Push Model: Broadcast to WebSocket immediately
                    // This informs the "Public Tape" / Frontend dashboards
                    messagingTemplate.convertAndSend("/topic/trades", trades);
                    log.info("Broadcasted {} trades for symbol {}", trades.size(), symbol);

                    // 3. Asynchronous Persistence via Virtual Threads
                    dbWriterExecutor.execute(() -> {
                        try {
                            persistenceService.persistTrades(trades);
                        } catch (Exception e) {
                            log.error("CRITICAL: Failed to persist trades for {}.", symbol, e);
                        }
                    });
                }
                return trades;
            } catch (Exception e) {
                log.error("Matching engine failure for order id: {}", order.id(), e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Recovery entry point: Restores orders into the book during system startup.
     */
    public void rebuildOrder(Order order) {
        String symbol = order.symbol();
        OrderBook book = books.computeIfAbsent(symbol, OrderBook::new);
        ExecutorService executor = engines.computeIfAbsent(symbol, this::createExecutor);

        executor.execute(() -> {
            log.debug("Recovery: Restoring order {} to {} book", order.id(), symbol);
            book.addOrderToBook(order);
        });
    }

    /**
     * Cancels a resting order asynchronously on the matching thread.
     */
    public CompletableFuture<Void> cancelOrder(String symbol, String orderId) {
        OrderBook book = books.get(symbol);
        if (book == null) {
            log.warn("Cancel failed: No book found for symbol {}", symbol);
            return CompletableFuture.completedFuture(null);
        }

        ExecutorService executor = engines.get(symbol);
        return CompletableFuture.runAsync(() -> {
            log.info("Processing cancellation for order: {}", orderId);
            book.cancel(orderId);
        }, executor);
    }

    private ExecutorService createExecutor(String symbol) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(50_000),
                r -> {
                    Thread t = new Thread(r, "engine-" + symbol);
                    t.setPriority(Thread.MAX_PRIORITY);
                    return t;
                });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Matching Engine...");
        engines.values().forEach(ExecutorService::shutdown);
        dbWriterExecutor.shutdown();
        try {
            if (!dbWriterExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("DB writer did not terminate gracefully. Forcing shutdown.");
                dbWriterExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbWriterExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Matching Engine offline.");
    }
}