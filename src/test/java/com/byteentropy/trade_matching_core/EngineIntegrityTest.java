package com.byteentropy.trade_matching_core;

import com.byteentropy.trade_matching_core.model.*;
import com.byteentropy.trade_matching_core.service.MatchingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-level integration test to verify the Matching Engine's 
 * core logic and integrity within the Spring Context.
 */
@SpringBootTest
class EngineIntegrityTest {

    @Autowired
    private MatchingEngine matchingEngine;

    // Mock the WebSocket template so we don't need a real message broker running
    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private static final long SCALE = 100_000_000L;

    @Test
    void testPricePriorityLogic() throws Exception {
        String symbol = "EURUSD_PRIORITY";
        long expensivePrice = (long) (1.10 * SCALE);
        long cheapPrice = (long) (1.05 * SCALE);
        long qty = 100 * SCALE;

        Order expensiveSell = new Order("Seller_A", symbol, Side.SELL, OrderType.LIMIT, expensivePrice, qty);
        Order cheapSell = new Order("Seller_B", symbol, Side.SELL, OrderType.LIMIT, cheapPrice, qty);

        matchingEngine.submitOrder(expensiveSell).get();
        matchingEngine.submitOrder(cheapSell).get();

        long buyerPrice = (long) (1.15 * SCALE);
        Order buyer = new Order("Buyer", symbol, Side.BUY, OrderType.LIMIT, buyerPrice, qty);
        List<Trade> trades = matchingEngine.submitOrder(buyer).get();

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).sellOrderId()).isEqualTo(cheapSell.id());
        assertThat(trades.get(0).price()).isEqualTo(cheapPrice);
    }

    @Test
    void testTimePriorityLogic() throws Exception {
        String symbol = "TIME_TEST";
        long price = (long) (50000 * SCALE);
        long qty = 10 * SCALE;

        Order seller1 = new Order("Seller_1", symbol, Side.SELL, OrderType.LIMIT, price, qty);
        Order seller2 = new Order("Seller_2", symbol, Side.SELL, OrderType.LIMIT, price, qty);

        matchingEngine.submitOrder(seller1).get();
        matchingEngine.submitOrder(seller2).get();

        Order buyer = new Order("Buyer", symbol, Side.BUY, OrderType.LIMIT, price, qty);
        List<Trade> trades = matchingEngine.submitOrder(buyer).get();

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).sellOrderId()).isEqualTo(seller1.id());
    }

    @Test
    void testPrecisionAtEightDecimals() throws Exception {
        String symbol = "PRECISION_TEST";
        long weirdPrice = 108551234L; 
        long weirdQty = 1000005678L;  

        Order sell = new Order("S1", symbol, Side.SELL, OrderType.LIMIT, weirdPrice, weirdQty);
        Order buy = new Order("B1", symbol, Side.BUY, OrderType.LIMIT, weirdPrice, weirdQty);

        matchingEngine.submitOrder(sell).get();
        List<Trade> trades = matchingEngine.submitOrder(buy).get();

        assertThat(trades).isNotEmpty();
        assertThat(trades.get(0).price()).isEqualTo(weirdPrice);
        assertThat(trades.get(0).quantity()).isEqualTo(weirdQty);
    }
}