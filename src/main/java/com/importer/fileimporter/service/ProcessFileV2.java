package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class ProcessFileV2 extends IProcessFile {

    private final PortfolioService portfolioService;
    private final TransactionService transactionService;

    public ProcessFileV2(PortfolioService portfolioService, TransactionService transactionService, FileImporterService fileImporterService) {
        super(fileImporterService);
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols, String portfolio) throws IOException {
        List<Map<?, ?>> rows = getRows(file);
        Map<String, CoinInformationResponse> transactionDetailInformation = getTransactionDetailInformation(rows, symbols, portfolio);

        return FileInformationResponse.builder()
                .amount(rows.size())
                .coinInformationResponse(transactionDetailInformation.values())
                .build();
    }

    private Map<String, CoinInformationResponse> getTransactionDetailInformation(List<Map<?, ?>> rows,
                                                                                 List<String> symbols,
                                                                                 String portfolioName) {
        Map<String, CoinInformationResponse> transactionsDetailsMap = new HashMap<>();
        Portfolio portfolio = portfolioService.findOrSave(portfolioName);
        rows.forEach(processRow(symbols, transactionsDetailsMap, portfolio));
        return transactionsDetailsMap;
    }

    private Consumer<Map<?, ?>> processRow(List<String> symbols,
                                           Map<String, CoinInformationResponse> transactionDetails,
                                           Portfolio portfolio) {
        return row -> {
            String pair = getPair(row);
            String symbol = getSymbolFromExecuted(row, symbols);
            if (symbol.isEmpty()) return;

            try {
                processTransactionRow(row, pair, symbol, transactionDetails, portfolio);
            } catch (Exception e) {
                log.error("Error processing row: {}", e.getMessage(), e);
            }
        };
    }

    private void processTransactionRow(Map<?, ?> row, String pair, String symbol,
                                       Map<String, CoinInformationResponse> transactionDetails,
                                       Portfolio portfolio) {
        String symbolPair = pair.replace(symbol, "");
        boolean isBuy = OperationUtils.isBuy(row);

        transactionDetails.computeIfAbsent(symbol, k -> createNewCoinInfo(symbol));
        CoinInformationResponse coinInfo = transactionDetails.get(symbol);

        updateCoinInfo(coinInfo, row, symbol, symbolPair, isBuy);
        saveTransaction(row, pair, symbol, portfolio, symbolPair, coinInfo);
    }

    private void saveTransaction(Map<?, ?> row, String pair, String symbol, Portfolio portfolio, String symbolPair, CoinInformationResponse coinInfo) {
        transactionService.saveTransaction(
                coinInfo.getCoinName(), symbolPair, getDate(row), pair,
                getSide(row), getPrice(row), getExecuted(row, symbol),
                getAmount(row, symbol), getFee(row), "Process File",
                portfolio
        );
    }

}
