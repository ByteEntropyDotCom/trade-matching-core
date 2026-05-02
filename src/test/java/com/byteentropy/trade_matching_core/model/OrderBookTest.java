package com.byteentropy.trade_matching_core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class OrderBookTest {

    private OrderBook orderBook;
    private static final long SCALE = 100_000_000L;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("BTCUSD");
    }

    @Test
    void shouldMatchExactOrders() {
        long price = 50000 * SCALE;
        long qty = 1 * SCALE;

        orderBook.match(new Order("user1", "BTCUSD", Side.SELL, OrderType.LIMIT, price, qty));
        List<Trade> trades = orderBook.match(new Order("user2", "BTCUSD", Side.BUY, OrderType.LIMIT, price, qty));

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).quantity()).isEqualTo(qty);
    }

    @Test
    void shouldRespectTimePriority() {
        long price = 50000 * SCALE;
        long qty = 1 * SCALE;

        // Create orders and capture them to get their actual IDs
        Order s1 = new Order("seller1", "BTCUSD", Side.SELL, OrderType.LIMIT, price, qty);
        Order s2 = new Order("seller2", "BTCUSD", Side.SELL, OrderType.LIMIT, price, qty);

        // Place them in the book
        orderBook.match(s1);
        orderBook.match(s2);

        // Taker Buys only 1 unit
        Order buyer = new Order("buyer", "BTCUSD", Side.BUY, OrderType.LIMIT, price, qty);
        List<Trade> trades = orderBook.match(buyer);

        // THEN: Verify the trade matched with the FIRST order (s1)
        assertThat(trades).hasSize(1);
        
        // Use s1.id() instead of hardcoding "seller1"
        assertThat(trades.get(0).sellOrderId()).isEqualTo(s1.id());
    }

    @Test
    void shouldSweepMultiplePriceLevelsWithMarketOrder() {
        long cheapPrice = 50000 * SCALE;
        long expensivePrice = 51000 * SCALE;
        long qty = 1 * SCALE;

        orderBook.match(new Order("seller1", "BTCUSD", Side.SELL, OrderType.LIMIT, cheapPrice, qty));
        orderBook.match(new Order("seller2", "BTCUSD", Side.SELL, OrderType.LIMIT, expensivePrice, qty));

        // Market Buy for 2 units sweeps the book
        Order marketOrder = new Order("taker", "BTCUSD", Side.BUY, OrderType.MARKET, 0L, 2 * SCALE);
        List<Trade> trades = orderBook.match(marketOrder);

        assertThat(trades).hasSize(2);
        assertThat(trades.get(0).price()).isEqualTo(cheapPrice);
        assertThat(trades.get(1).price()).isEqualTo(expensivePrice);
    }

    @Test
    void shouldPreventSelfTrade() {
        long price = 50000 * SCALE;
        long qty = 1 * SCALE;

        // User1 puts a limit sell
        orderBook.match(new Order("maker1", "user1", "BTCUSD", Side.SELL, OrderType.LIMIT, price, qty, System.currentTimeMillis()));
        
        // User1 tries to buy their own order
        List<Trade> trades = orderBook.match(new Order("taker1", "user1", "BTCUSD", Side.BUY, OrderType.LIMIT, price, qty, System.currentTimeMillis()));

        assertThat(trades).isEmpty(); // STP should prevent this match
    }

    @Test
    void shouldHandlePartialFills() {
        long price = 50000 * SCALE;
        long largeQty = 10 * SCALE;
        long smallQty = 3 * SCALE;

        orderBook.match(new Order("seller", "BTCUSD", Side.SELL, OrderType.LIMIT, price, largeQty));
        List<Trade> trades = orderBook.match(new Order("buyer", "BTCUSD", Side.BUY, OrderType.LIMIT, price, smallQty));
        
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).quantity()).isEqualTo(smallQty);
    }
}