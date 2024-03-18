package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.TransactionId;
import com.importer.fileimporter.repository.TransactionRepository;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
import com.importer.fileimporter.utils.DateUtils;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CalculateAmountSpent calculateAmountSpent;
    private final CoinInformationService coinInformationService;

    public Page<Transaction> getTransactionsByRangeDate(String symbol, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (symbol != null && (startDate == null && endDate == null)) {
            return getAllBySymbol(symbol, pageable);
        }
        return transactionRepository.findAllBySymbolOrSymbolIsNullAndTransactionIdDateUtcBetween(symbol, startDate.atStartOfDay(),
                endDate.plusDays(1L).atStartOfDay().minusSeconds(1L), pageable);
    }

    public Page<Transaction> getAllBySymbol(String symbol, Pageable pageable) {
        return transactionRepository.findAllBySymbol(symbol, pageable);
    }

    public CoinInformationResponse getTransactionsInformation(String symbol, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        List<Transaction> transactions = getTransactionsByRangeDate(symbol, startDate, endDate, pageable).getContent();
        BigDecimal amountSpent = calculateAmountSpent.execute(symbol, transactions);

        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        transactions.forEach(a ->
                amount.set(OperationUtils.sumAmount(amount, a.getTransactionId().getExecuted(), a.getTransactionId().getSide())));

        CoinInformationResponse response = CoinInformationResponse.builder()
                .amount(amount.get())
                .coinName(symbol)
                .totalTransactions(transactions.size())
                .avgEntryPrice(new HashMap<>())
                .usdSpent(amountSpent)
                .spent(new HashMap<>())
                .build();
        coinInformationService.calculateAvgEntryPrice(response, transactions);
        return response;
    }

    public Transaction saveTransaction(String coinName,
                                       String symbolPair, String date,
                                       String pair, String side,
                                       BigDecimal price, BigDecimal executed,
                                       BigDecimal amount, BigDecimal fee, String origin) {
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
                .payedWith(symbolPair)
                .payedAmount(amount)
                .created(LocalDateTime.now())
                .createdBy(origin)
                .feeAmount(fee)
                .modified(LocalDateTime.now())
                .modifiedBy(origin)
                .build();

        return transactionRepository.save(transaction);
    }
}
