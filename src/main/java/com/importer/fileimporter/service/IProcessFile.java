package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.utils.ProcessFileServiceUtils;
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
public abstract class IProcessFile {

    private final FileImporterService fileImporterService;

    protected List<Map<?, ?>> getRows(MultipartFile file) throws IOException {
        return fileImporterService.getRows(file);
    }

    CoinInformationResponse createNewCoinInfo(String symbol) {
        return CoinInformationResponse.createEmpty(symbol);
    }

    void updateCoinInfo(CoinInformationResponse coinInfo, Map<?, ?> row, String symbol, String symbolPair, boolean isBuy) {
        coinInfo.setTotalExecuted(calculateAmount(coinInfo.getAmount(), isBuy, getExecuted(row, symbol)));
        updateSpentAndAvgPrice(coinInfo, row, symbolPair, isBuy);
        coinInfo.addRows(row);
    }

    void updateSpentAndAvgPrice(CoinInformationResponse coinInfo, Map<?, ?> row, String symbolPair, boolean isBuy) {
        STABLE.stream()
                .filter(symbolPair::contains)
                .findFirst()
                .ifPresent(stableCoin -> {
                    BigDecimal amount = getAmount(row, stableCoin);
                    BigDecimal updatedSpent = updateAmountSpent(coinInfo.getStableTotalCost(), amount, isBuy);
                    coinInfo.setStableTotalCost(updatedSpent);
                });

        calculateSpent(getAmount(row, symbolPair), coinInfo, symbolPair, isBuy);
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

    String getSymbolFromExecuted(Map<?, ?> row, List<String> symbols) {
        return ProcessFileServiceUtils.getSymbolFromExecuted(row, symbols);
    }

    String getDate(Map<?, ?> row) {
        return ProcessFileServiceUtils.getDate(row);
    }

    String getPair(Map<?, ?> row) {
        return ProcessFileServiceUtils.getPair(row);
    }

    BigDecimal getExecuted(Map<?, ?> row, String coinName) {
        return ProcessFileServiceUtils.getExecuted(row, coinName);
    }

    BigDecimal getAmount(Map<?, ?> row, String symbolPair) {
        return ProcessFileServiceUtils.getAmount(row, symbolPair);
    }

    BigDecimal getFee(Map<?, ?> row) {
        return ProcessFileServiceUtils.getFee(row);
    }

    BigDecimal getPrice(Map<?, ?> row) {
        return ProcessFileServiceUtils.getPrice(row);
    }

    String getSide(Map<?, ?> row) {
        return ProcessFileServiceUtils.getSide(row);
    }

}
