package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.TransactionData;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.importer.fileimporter.utils.OperationUtils.STABLE;

@Service
@AllArgsConstructor
public abstract class ProcessFile {

    private final FileImporterService fileImporterService;
    protected final TransactionAdapterFactory transactionAdapterFactory;

    protected List<Map<?, ?>> getRows(MultipartFile file) throws IOException {
        return fileImporterService.getRows(file);
    }

    CoinInformationResponse createNewCoinInfo(String symbol) {
        return CoinInformationResponse.createEmpty(symbol);
    }

    void updateCoinInfo(CoinInformationResponse coinInfo, TransactionData transactionData, boolean isBuy) {
        coinInfo.setTotalExecuted(calculateAmount(coinInfo.getAmount(), isBuy, transactionData.getExecuted()));
        updateSpentAndAvgPrice(coinInfo, transactionData, isBuy);
        coinInfo.addRows(transactionData);
    }

    void updateSpentAndAvgPrice(CoinInformationResponse coinInfo, TransactionData transactionData, boolean isBuy) {
        String paidWith = transactionData.getPaidWith();
        STABLE.stream()
                .filter(paidWith::contains)
                .findFirst()
                .ifPresent(stableCoin -> {
                    BigDecimal amount = transactionData.getAmount();
                    BigDecimal updatedSpent = updateAmountSpent(coinInfo.getStableTotalCost(), amount, isBuy);
                    coinInfo.setStableTotalCost(updatedSpent);
                });

        calculateSpent(transactionData.getAmount(), coinInfo, paidWith, isBuy);
    }

    BigDecimal updateAmountSpent(BigDecimal currentSpent, BigDecimal amount, boolean isBuy) {
        return isBuy ? currentSpent.add(amount) : currentSpent.subtract(amount);
    }

    void calculateSpent(BigDecimal amount, CoinInformationResponse coinInfo, String symbolPair, boolean isBuy) {
        coinInfo.getSpent().merge(symbolPair, amount,
                (current, newAmount) -> isBuy ? current.add(newAmount) : current.subtract(newAmount));
    }

    BigDecimal calculateAmount(BigDecimal currentAmount, boolean isBuy, BigDecimal amountToAdjust) {
        return isBuy ?
                currentAmount.add(amountToAdjust) :
                currentAmount.subtract(amountToAdjust);
    }

    protected TransactionData getAdapter(Map<?, ?> row, String portfolioName) {
        final String name = portfolioName == null ? "Binance" : portfolioName;
        return transactionAdapterFactory.createAdapter(row, name);
    }

}
