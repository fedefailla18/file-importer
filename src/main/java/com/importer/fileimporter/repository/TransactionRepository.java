package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findAllBySymbol(String symbol, Pageable pageable);

    List<Transaction> findAllBySymbol(String symbol);

    List<Transaction> findAllByPortfolio(Portfolio portfolio);

    List<Transaction> findAllByPortfolioAndSymbol(Portfolio portfolio, String symbol);

    @Query("SELECT t FROM Transaction t " +
            "WHERE (:symbol IS NULL OR t.symbol = :symbol) " +
            "AND (CAST(:portfolioId AS string) IS NULL OR t.portfolio.id = :portfolioId) " +
            "AND (CAST(:startDate AS string) IS NULL OR t.dateUtc >= CAST(:startDate AS timestamp)) " +
            "AND (CAST(:endDate AS string) IS NULL OR t.dateUtc <= CAST(:endDate AS timestamp)) " +
            "ORDER BY t.dateUtc DESC")
    Page<Transaction> findBySymbolAndPortfolioAndDateRange(@Param("symbol") String symbol,
                                                           @Param("portfolioId") UUID portfolioId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate,
                                                           Pageable pageable);
    List<Transaction> findByPortfolioIn(List<Portfolio> portfolios);
    Page<Transaction> findByPortfolioIn(List<Portfolio> portfolios, Pageable pageable);

    void deleteAllByPortfolio(Portfolio portfolio);
}
