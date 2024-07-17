package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.TransactionId;
import com.importer.fileimporter.utils.DateUtils;
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
public class CoinInformationService {

    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceService;

    public void calculateAvgEntryPrice(CoinInformationResponse detail, List<Transaction> transactionList) {
        transactionList.forEach(e -> {
            TransactionId transactionId = e.getTransactionId();
            boolean isBuy = OperationUtils.isBuy(transactionId.getSide());

            calculateAvgEntryPrice(detail, transactionId.getDateUtc(), e.getPayedWith(),
                    isBuy, transactionId.getExecuted(), transactionId.getPrice());
        });
    }
    public void calculateAvgEntryPrice(CoinInformationResponse detail, String date, String symbolPair, boolean isBuy, BigDecimal executed, BigDecimal price) {
        LocalDateTime dateTime = DateUtils.getLocalDateTime(date);
        calculateAvgEntryPrice(detail, dateTime, symbolPair, isBuy, executed, price);
    }

    public void calculateAvgEntryPrice(CoinInformationResponse detail, LocalDateTime date, String symbolPair, boolean isBuy, BigDecimal executed, BigDecimal price) {
        if (OperationUtils.hasStable(symbolPair).isPresent()) {
            detail.setAvgEntryPrice(symbolPair, price, executed, isBuy);
        } else {
            log.info("No a Stable Coin transaction. " + symbolPair);

            BigDecimal priceInUsdt = getSymbolHistoricPriceService.getPriceInUsdt(symbolPair, price, date);
            // save transaction. store priceInUsdt so it's easier to calculate in the future
            if (0 <= priceInUsdt.doubleValue()) {
                detail.setAvgEntryPrice("USDT", priceInUsdt, executed, isBuy);
            } else {
                log.warn("Check transaction for date " + date);
            }
        }
    }
}
