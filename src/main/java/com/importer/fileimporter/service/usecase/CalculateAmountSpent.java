package com.importer.fileimporter.service.usecase;

import com.importer.fileimporter.entity.PriceHistory;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.repository.TransactionRepository;
import com.importer.fileimporter.service.GetSymbolHistoricPriceHelper;
import com.importer.fileimporter.service.PriceHistoryService;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Service
public class CalculateAmountSpent {

    private final TransactionRepository transactionRepository;
    private final PriceHistoryService priceHistoryService;
    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceService;

    public BigDecimal execute(String symbol, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        List<Transaction> transactions = transactionRepository.findAllBySymbolOrSymbolIsNullAndTransactionIdDateUtcBetween(symbol, startDate.atStartOfDay(),
                endDate.plusDays(1L).atStartOfDay().minusSeconds(1L), pageable).getContent();

        return execute(symbol, transactions);
    }

    public BigDecimal execute(String symbol, List<Transaction> transactions) {
        AtomicReference<BigDecimal> amountSpent = new AtomicReference<>(BigDecimal.ZERO);
        transactions.forEach(transaction -> {
            getAmountSpentPerTransaction(symbol, amountSpent, transaction);
        });
        return amountSpent.get();
    }

    private void getAmountSpentPerTransaction(String symbol, AtomicReference<BigDecimal> amountSpent, Transaction transaction) {
        String payedWithSymbol = transaction.getPayedWith();
        String side = transaction.getTransactionId().getSide();
        BigDecimal payedAmount = transaction.getPayedAmount();

        if (OperationUtils.isStable(payedWithSymbol)) {
            amountSpent.set(OperationUtils.sumAmount(amountSpent, payedAmount, side));
        } else {
            BigDecimal priceInUsdt;
            Optional<PriceHistory> priceHistory = priceHistoryService.findData(symbol, payedWithSymbol, transaction.getTransactionId().getDateUtc());
            if (priceHistory.isEmpty()) {
                priceInUsdt = getSymbolHistoricPriceService.getPriceInUsdt(payedWithSymbol,
                        transaction.getPayedAmount(),
                        transaction.getTransactionId().getDateUtc());
            } else {
                priceInUsdt = priceHistory.get().getHigh();
            }
            amountSpent.set(OperationUtils.sumAmount(amountSpent, priceInUsdt, side));
        }
    }
}
