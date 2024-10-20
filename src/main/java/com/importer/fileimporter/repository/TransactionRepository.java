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
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findAllBySymbol(String symbol, Pageable pageable);

    List<Transaction> findAllBySymbol(String symbol);

    List<Transaction> findAllByPortfolio(Portfolio portfolio);

    @Query("SELECT t FROM Transaction t " +
            "WHERE (:symbol IS NULL OR t.symbol = :symbol) " +
            "AND (:portfolioId IS NULL OR t.portfolio.id = COALESCE(:portfolioId, t.portfolio.id)) " +
            "AND (:startDate IS NULL OR DATE(t.dateUtc) >= :startDate) " +
            "AND (:endDate IS NULL OR DATE(t.dateUtc) <= :endDate) " +
            "ORDER BY t.dateUtc DESC")
    Page<Transaction> findBySymbolAndPortfolioAndDateRange(@Param("symbol") String symbol,
                                                           @Param("portfolioId") UUID portfolioId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate,
                                                           Pageable pageable);

//    @Query("SELECT t FROM Transaction t " +
//            "WHERE (:symbol IS NULL OR t.symbol = :symbol) " +
//            "AND (:portfolioName IS NULL OR t.portfolio.name = :portfolioName) " +
//            "AND (:side IS NULL OR t.side = :side) " +
//            "AND (:paidWith IS NULL OR t.payedWith = :paidWith) " +
//            "AND (:startDate IS NULL OR DATE(t.dateUtc) >= :startDate) " +
//            "AND (:endDate IS NULL OR DATE(t.dateUtc) <= :endDate) " +
//            "AND (:paidAmount IS NULL OR " +
//            "(CASE :paidAmountOperator WHEN '>' THEN t.payedAmount > :paidAmount " +
//            "WHEN '=' THEN t.payedAmount = :paidAmount " +
//            "WHEN '<' THEN t.payedAmount < :paidAmount ELSE true END)) " +
//            "ORDER BY t.dateUtc DESC")
//    Page<Transaction> filterTransactions(@Param("symbol") String symbol,
//                                         @Param("portfolioName") String portfolioName,
//                                         @Param("side") String side,
//                                         @Param("paidWith") String paidWith,
//                                         @Param("startDate") LocalDate startDate,
//                                         @Param("endDate") LocalDate endDate,
//                                         @Param("paidAmountOperator") String paidAmountOperator,
//                                         @Param("paidAmount") BigDecimal paidAmount,
//                                         Pageable pageable);
}
