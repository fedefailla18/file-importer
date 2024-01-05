package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.TransactionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, TransactionId> {

    Page<Transaction> findAllBySymbol(String symbol, Pageable pageable);
}
