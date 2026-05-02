package com.byteentropy.trade_matching_core.repository;

import com.byteentropy.trade_matching_core.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    Optional<OrderEntity> findByClientOrderId(String clientOrderId);
}