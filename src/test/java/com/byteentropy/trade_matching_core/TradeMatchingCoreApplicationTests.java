package com.byteentropy.trade_matching_core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradeMatchingCoreApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    // Mock the WebSocket template to ensure smooth context loading during tests
    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void contextLoads() {
        // Verifies that the Spring container starts successfully with all new beans
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void verifyVirtualThreadsEnabled() {
        // Verifies that the 'spring.threads.virtual.enabled' property is picked up
        // This is crucial for your dbWriterExecutor performance
        String virtualThreadsProperty = applicationContext.getEnvironment()
                .getProperty("spring.threads.virtual.enabled");
        
        assertThat(virtualThreadsProperty).isEqualTo("true");
    }

    @Test
    void verifyWebSocketBeansPresent() {
        // Optional: Verify that your new WebSocket infrastructure is actually loaded
        assertThat(applicationContext.containsBean("webSocketConfig")).isTrue();
    }
}