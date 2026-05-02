package com.byteentropy.trade_matching_core.repository;

import com.byteentropy.trade_matching_core.model.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, String> {}