package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.utils.OperationUtils;
import com.importer.fileimporter.utils.ProcessFileServiceUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
    private final CoinInformationService coinInformationService;
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

    private Consumer<Map<?, ?>> processRow(List<String> symbols,
                                           Map<String, CoinInformationResponse> transactionsDetailsMap) {
        return row -> {
            String pair = getPair(row);
            try {
                String symbol = getSymbolFromExecuted(row, symbols);
                if (Strings.isNotEmpty(symbol)) {
                    String symbolPair = pair.replace(symbol, "");
                    boolean isBuy = OperationUtils.isBuy(row);

                    if (!transactionsDetailsMap.containsKey(symbol)) {
                        transactionsDetailsMap.computeIfAbsent(symbol, k -> {
                            CoinInformationResponse coinInformationResponse = CoinInformationResponse.createEmpty(k);
                            coinInformationResponse
                                    .setAmount(calculateAmount(coinInformationResponse.getAmount(), isBuy, getExecuted(row, symbol)));

                            // TODO:
                            STABLE.stream()
                                    .filter(symbolPair::contains)
                                    .findFirst()
                                    .ifPresent(value -> {
                                        final BigDecimal amount = getAmount(row, value);
                                        final BigDecimal amountSpent = getAmountSpent(coinInformationResponse.getUsdSpent(), amount, isBuy);
                                        coinInformationResponse.setUsdSpent(amountSpent);
                                    });

                            calculateSpent(getAmount(row, symbolPair), coinInformationResponse, symbolPair, isBuy);

                            coinInformationResponse.addRows(row);

                            transactionService.saveTransaction(coinInformationResponse.getCoinName(), symbolPair, getDate(row), pair,
                                    getSide(row), getPrice(row), getExecuted(row, coinInformationResponse.getCoinName()),
                                    getAmount(row, coinInformationResponse.getCoinName()), getFee(row), "Process File - New Coin");
                            return coinInformationResponse;
                        });
                        return;
                    }
                    CoinInformationResponse existingCoinInfo = transactionsDetailsMap.get(symbol);
                    existingCoinInfo.setAmount(calculateAmount(existingCoinInfo.getAmount(), isBuy, getExecuted(row, symbol)));

                    STABLE.stream()
                            .filter(symbolPair::contains)
                            .findFirst()
                            .ifPresent(value -> {
                                BigDecimal amount = getAmount(row, value);
                                BigDecimal amountSpent = getAmountSpent(existingCoinInfo.getUsdSpent(), amount, isBuy);
                                existingCoinInfo.setUsdSpent(amountSpent);
                            });

                    calculateSpent(getAmount(row, symbolPair), existingCoinInfo, symbolPair, isBuy);
                    existingCoinInfo.addRows(row);
                    transactionService.saveTransaction(existingCoinInfo.getCoinName(), symbolPair, getDate(row), pair, row.get("Side").toString(), getPrice(row),
                            getExecuted(row, existingCoinInfo.getCoinName()), getAmount(row, existingCoinInfo.getCoinName()), getFee(row), "Process File");
                }
            } catch (Exception e) {
                log.info(e.getMessage(), e);
            }
        };
    }

    private void calculateSpent(BigDecimal amount, CoinInformationResponse a, String symbolPair, boolean isBuy) {
        a.getSpent().computeIfAbsent(symbolPair, k -> new BigDecimal(0));
        if (isBuy) {
            a.getSpent().computeIfPresent(symbolPair, (k, v) -> v.add(amount));
        } else {
            a.getSpent().computeIfPresent(symbolPair, (k, v) -> v.subtract(amount));
        }
    }

    private BigDecimal getAmountSpent(BigDecimal usdSpent, BigDecimal amount, boolean isBuy) {
        if (isBuy) {
            return usdSpent.add(amount);
        } else {
            return usdSpent.subtract(amount);
        }
    }

    private BigDecimal calculateAmount(BigDecimal amount, boolean isBuy, BigDecimal amountAdded) {
        if (isBuy) {
            return amount.add(amountAdded);
        } else {
            return amount.subtract(amountAdded);
        }
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

    // TODO: retrieve the symbol that affected the fee
    private BigDecimal getFee(Map<?, ?> row) {
        return ProcessFileServiceUtils.getFee(row);
    }

    private String getFeeSymbol(String feeString, String symbol) {
        return ProcessFileServiceUtils.getFeeSymbol(feeString, symbol);
    }

    private BigDecimal getPrice(Map<?, ?> row) {
        return ProcessFileServiceUtils.getPrice(row);
    }

    private BigDecimal getBigDecimalWithScale(Number number) {
        return ProcessFileServiceUtils.getBigDecimalWithScale(number);
    }

    private String getSide(Map<?, ?> row) {
        return ProcessFileServiceUtils.getSide(row);
    }
}
