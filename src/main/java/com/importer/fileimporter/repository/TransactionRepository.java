package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.TransactionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, TransactionId> {

    Page<Transaction> findAllBySymbol(String symbol, Pageable pageable);

    List<Transaction> findAllBySymbol(String symbol);

    @Query(value = "select transaction " +
            "from Transaction transaction " +
            "where ((:startDate is null and :endDate is null) or " +
            "       transaction.transactionId.dateUtc BETWEEN :startDate AND :endDate) " +
            "and (transaction.symbol is null or transaction.symbol = :symbol)")
    Page<Transaction> findAllBySymbolAndDateRange(@Param("symbol") String symbol,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate, Pageable pageable);

    Page<Transaction> findAllBySymbolOrSymbolIsNullAndTransactionIdDateUtcBetween(@Param("symbol") String symbol,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate, Pageable pageable);

}
