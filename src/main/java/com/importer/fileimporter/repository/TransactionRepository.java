package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findAllBySymbolAndSymbolPair(String symbol, String symbolPair);
}
