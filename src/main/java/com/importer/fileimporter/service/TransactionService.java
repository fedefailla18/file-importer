package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.TransactionId;
import com.importer.fileimporter.repository.TransactionRepository;
import com.importer.fileimporter.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;

    public Page<Transaction> getTransactionsByFilters(String symbol, String portfolioName, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Optional<Portfolio> portfolio = portfolioService.getByName(portfolioName);
        UUID portfolioId = portfolio.map(Portfolio::getId).orElse(null);
        if (portfolio.isEmpty()) {
            return transactionRepository.findBySymbolAndDateRange(symbol, startDate, endDate, pageable);
        }

        return transactionRepository.findBySymbolAndPortfolioAndDateRange(symbol, portfolioId, startDate, endDate, pageable);
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

    public Transaction saveTransaction(String coinName,
                                       String symbolPair, String date,
                                       String pair, String side,
                                       BigDecimal price, BigDecimal executed,
                                       BigDecimal amount, BigDecimal fee, String origin) {
        return saveTransaction(coinName, symbolPair, date, pair, side, price, executed, amount, fee, origin, null);
    }

    public Transaction saveTransaction(String coinName,
                                       String symbolPair, String date,
                                       String pair, String side,
                                       BigDecimal price, BigDecimal executed,
                                       BigDecimal amount, BigDecimal fee, String origin,
                                       Portfolio portfolio) {
        LocalDateTime dateTime = DateUtils.getLocalDateTime(date);

        TransactionId transactionId = TransactionId.builder()
                .dateUtc(dateTime)
                .pair(pair)
                .executed(executed)
                .side(side)
                .price(price)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
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

        return transactionRepository.save(transaction);
    }

    public void deleteTransactions() {
        transactionRepository.deleteAll();
    }

    public List<Transaction> findByPortfolio(Portfolio portfolio) {
        return transactionRepository.findAllByPortfolio(portfolio);
    }
}
