# Trade Matching Core

A high-performance, sharded crypto/stock matching engine built with Java 21 and Spring Boot.

## Features
*   **L3 Matching:** Price-Time Priority (FIFO) matching algorithm.
*   **Symbol Sharding:** Each trading pair runs on its own dedicated high-priority thread.
*   **Push Model:** Real-time trade broadcasting via WebSockets (STOMP).
*   **Fixed-Point Math:** Uses 8-decimal scale (10^8) for precision.
*   **Virtual Threads:** Non-blocking database I/O using Java 21 Virtual Threads.
*   **Self-Trade Prevention (STP):** Internal logic to prevent wash trading.

## Performance
The engine is designed to handle thousands of orders per second per symbol by isolating the matching logic from I/O tasks.