package com.byteentropy.trade_matching_core.controller;

import com.byteentropy.trade_matching_core.dto.OrderRequest;
import com.byteentropy.trade_matching_core.model.Trade;
import com.byteentropy.trade_matching_core.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<List<Trade>> placeOrder(
            @Valid @RequestBody OrderRequest request) {

        return orderService.processOrder(request);
    }
}