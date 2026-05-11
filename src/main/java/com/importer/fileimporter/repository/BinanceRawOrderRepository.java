package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.BinanceRawOrder;
import com.importer.fileimporter.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BinanceRawOrderRepository extends JpaRepository<BinanceRawOrder, UUID> {
    Page<BinanceRawOrder> findByUserAndSymbol(User user, String symbol, Pageable pageable);
    Optional<BinanceRawOrder> findByUserAndSymbolAndOrderId(User user, String symbol, Long orderId);
}
