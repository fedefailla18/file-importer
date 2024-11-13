package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.repository.TransactionRepository;
import com.importer.fileimporter.specification.TransactionSpecifications;
import com.importer.fileimporter.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public Page<Transaction> filterTransactions(String symbol, String portfolioName, String side,
                                                String paidWith, String paidAmountOperator, BigDecimal paidAmount,
                                                LocalDate startDate, LocalDate endDate,
                                                UUID userId, Pageable pageable) {

        log.info("Filtering transactions with parameters: symbol={}, portfolioName={}, side={}, paidWith={}, " +
                        "paidAmountOperator={}, paidAmount={}, startDate={}, endDate={}",
                symbol, portfolioName, side, paidWith, paidAmountOperator, paidAmount, startDate, endDate);

        Specification<Transaction> specification = TransactionSpecifications.getSpecWithFilters(symbol, portfolioName, side,
                paidWith, paidAmountOperator, paidAmount, startDate, endDate, userId);
        Page<Transaction> result = transactionRepository.findAll(specification, pageable);
        log.info("Found {} transactions", result.getTotalElements());

        return result;
    }

    public Page<Transaction> getAllBySymbol(String symbol, Pageable pageable) {
        return transactionRepository.findAllBySymbol(symbol, pageable);
    }

    public List<Transaction> getAllBySymbol(String symbol) {
        return transactionRepository.findAllBySymbol(symbol);
    }

    public List<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    public Page<Transaction> getAll(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public void saveTransaction(String coinName,
                                String symbolPair, String date,
                                String pair, String side,
                                BigDecimal price, BigDecimal executed,
                                BigDecimal amount, BigDecimal fee, String origin) {
        saveTransaction(coinName, symbolPair, date, pair, side, price, executed, amount, fee, origin, null);
    }

    public Transaction saveTransaction(String coinName,
                                       String symbolPair, String date,
                                       String pair, String side,
                                       BigDecimal price, BigDecimal executed,
                                       BigDecimal amount, BigDecimal fee, String origin,
                                       Portfolio portfolio) {
        LocalDateTime dateTime = DateUtils.getLocalDateTime(date);

        Transaction transaction = Transaction.builder()
                .dateUtc(dateTime)
                .pair(pair)
                .executed(executed)
                .side(side)
                .price(price)
                .symbol(coinName)
                .paidWith(symbolPair)
                .paidAmount(amount)
                .created(LocalDateTime.now())
                .createdBy(origin)
                .feeAmount(fee)
                .modified(LocalDateTime.now())
                .modifiedBy(origin)
                .portfolio(portfolio)
                .build();

        return save(transaction);
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void deleteTransactions() {
        transactionRepository.deleteAll();
    }

    public List<Transaction> findByPortfolio(Portfolio portfolio) {
        return transactionRepository.findAllByPortfolio(portfolio);
    }
}
