package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.TransactionId;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class CoinInformationHelper {

    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceHelper;

    public void calculateAvgEntryPrice(CoinInformationResponse detail, List<Transaction> transactionList) {
        transactionList.forEach(e -> {
            TransactionId transactionId = e.getTransactionId();
            boolean isBuy = OperationUtils.isBuy(transactionId.getSide());

            LocalDateTime dateTime = transactionId.getDateUtc();
            String payedWith = e.getPayedWith();
            BigDecimal executed = transactionId.getExecuted();
            BigDecimal price = transactionId.getPrice();

            calculateAvgEntryPrice(detail, dateTime, payedWith, isBuy, executed, price);
        });
        // Calculate the final average price
        detail.calculateAvgPrice();
    }

    public void calculateAvgEntryPrice(CoinInformationResponse detail,
                                       LocalDateTime date,
                                       String payedWith,
                                       boolean isBuy,
                                       BigDecimal executed,
                                       BigDecimal price) {
        if (OperationUtils.isStable(payedWith)) {
            detail.setAvgEntryPrice(payedWith, price, executed, isBuy);
        } else {
            log.info("Not a Stable Coin transaction. " + payedWith);

            BigDecimal priceInUsdt = getSymbolHistoricPriceHelper.getPriceInUsdt(payedWith, price, date);
            if (priceInUsdt.compareTo(BigDecimal.ZERO) >= 0) {
                detail.setAvgEntryPrice("USDT", priceInUsdt, executed, isBuy);
            } else {
                log.warn("Check transaction for date " + date);
            }
        }
    }
}
