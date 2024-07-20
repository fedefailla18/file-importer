package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.utils.OperationUtils;
import com.importer.fileimporter.utils.ProcessFileServiceUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.importer.fileimporter.utils.OperationUtils.STABLE;
import static com.importer.fileimporter.utils.OperationUtils.SYMBOL;

@AllArgsConstructor
@Service
@Slf4j
public class ProcessFile {

    private final FileImporterService fileImporterService;
    private final TransactionService transactionService;
    private final SymbolService symbolService;

    public FileInformationResponse processFile(MultipartFile file) throws IOException {
        return processFile(file, SYMBOL);
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols) throws IOException {
        if (symbols == null || symbols.isEmpty()) {
            symbols = symbolService.getAllSymbols();
        }
        List<Map<?, ?>> rows = fileImporterService.getRows(file);
        Map<String, CoinInformationResponse> transactionDetailInformation = getTransactionDetailInformation(rows, symbols);

        return FileInformationResponse.builder()
                .amount(rows.size())
                .coinInformationResponse(transactionDetailInformation.values())
                .build();
    }

    private Map<String, CoinInformationResponse> getTransactionDetailInformation(List<Map<?, ?>> rows, List<String> symbols) {
        Map<String, CoinInformationResponse> transactionsDetailsMap = new HashMap<>();

        rows.forEach(processRow(symbols, transactionsDetailsMap));
        return transactionsDetailsMap;
    }

    private Consumer<Map<?, ?>> processRow(List<String> symbols, Map<String, CoinInformationResponse> transactionDetails) {
        return row -> {
            String pair = getPair(row);
            String symbol = getSymbolFromExecuted(row, symbols);
            if (symbol.isEmpty()) return;

            try {
                processTransactionRow(row, pair, symbol, transactionDetails);
            } catch (Exception e) {
                log.error("Error processing row: {}", e.getMessage(), e);
            }
        };
    }

    private void processTransactionRow(Map<?, ?> row, String pair, String symbol, Map<String, CoinInformationResponse> transactionDetails) {
        String symbolPair = pair.replace(symbol, "");
        boolean isBuy = OperationUtils.isBuy(row);

        transactionDetails.computeIfAbsent(symbol, k -> createNewCoinInfo(symbol, row, symbolPair, isBuy));
        CoinInformationResponse coinInfo = transactionDetails.get(symbol);

        updateCoinInfo(coinInfo, row, symbol, symbolPair, isBuy);
        transactionService.saveTransaction(
                coinInfo.getCoinName(), symbolPair, getDate(row), pair,
                getSide(row), getPrice(row), getExecuted(row, symbol),
                getAmount(row, symbol), getFee(row), "Process File"
        );
    }

    private CoinInformationResponse createNewCoinInfo(String symbol, Map<?, ?> row, String symbolPair, boolean isBuy) {
        return CoinInformationResponse.createEmpty(symbol);
    }

    private void updateCoinInfo(CoinInformationResponse coinInfo, Map<?, ?> row, String symbol, String symbolPair, boolean isBuy) {
        coinInfo.setTotalExecuted(calculateAmount(coinInfo.getAmount(), isBuy, getExecuted(row, symbol)));
        updateSpentAndAvgPrice(coinInfo, row, symbolPair, isBuy);
        coinInfo.addRows(row);
    }

    private void updateSpentAndAvgPrice(CoinInformationResponse coinInfo, Map<?, ?> row, String symbolPair, boolean isBuy) {
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

    private BigDecimal updateAmountSpent(BigDecimal currentSpent, BigDecimal amount, boolean isBuy) {
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

    private String getSymbolFromExecuted(Map<?, ?> row, List<String> symbols) {
        return ProcessFileServiceUtils.getSymbolFromExecuted(row, symbols);
    }

    private String getDate(Map<?, ?> row) {
        return ProcessFileServiceUtils.getDate(row);
    }

    private String getPair(Map<?, ?> row) {
        return ProcessFileServiceUtils.getPair(row);
    }

    private BigDecimal getExecuted(Map<?, ?> row, String coinName) {
        return ProcessFileServiceUtils.getExecuted(row, coinName);
    }

    private BigDecimal getAmount(Map<?, ?> row, String symbolPair) {
        return ProcessFileServiceUtils.getAmount(row, symbolPair);
    }

    private BigDecimal getFee(Map<?, ?> row) {
        return ProcessFileServiceUtils.getFee(row);
    }

    private BigDecimal getPrice(Map<?, ?> row) {
        return ProcessFileServiceUtils.getPrice(row);
    }

    private String getSide(Map<?, ?> row) {
        return ProcessFileServiceUtils.getSide(row);
    }
}
