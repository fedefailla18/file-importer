package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.dto.TransactionData;
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

    public ProcessFileV2(PortfolioService portfolioService,
                         TransactionService transactionService,
                         FileImporterService fileImporterService,
                         TransactionAdapterFactory transactionAdapterFactory) {
        super(fileImporterService, transactionAdapterFactory);
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols, String portfolio) throws IOException {
        List<Map<?, ?>> rows = getRows(file);
        Map<String, CoinInformationResponse> transactionDetailInformation = getTransactionDetailInformation(rows, symbols, portfolio);

        return FileInformationResponse.builder()
                .portfolio(portfolio)
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
            TransactionData transactionData = getAdapter(row, portfolio.getName());
            String symbol = transactionData.getSymbol();
            log.info(this.getClass().getName() + " - Processing row for: " + symbol);
            if (symbol.isEmpty()) {
                return;
            }
            try {
                processTransactionRow(transactionData, symbol, transactionDetails, portfolio);
            } catch (Exception e) {
                log.error("Error processing row: {}", e.getMessage(), e);
            }
        };
    }

    private void processTransactionRow(TransactionData transactionData, String symbol,
                                       Map<String, CoinInformationResponse> transactionDetails,
                                       Portfolio portfolio) {
        boolean isBuy = OperationUtils.isBuy(transactionData.getSide());

        transactionDetails.computeIfAbsent(symbol, k -> createNewCoinInfo(symbol));
        CoinInformationResponse coinInfo = transactionDetails.get(symbol);

        updateCoinInfo(coinInfo, transactionData, isBuy);
        saveTransaction(transactionData, portfolio);
    }

    private void saveTransaction(TransactionData transactionData, Portfolio portfolio) {
        transactionService.saveTransaction(
                transactionData.getCoinName(), transactionData.getPaidWith(), transactionData.getDate(), transactionData.getPair(),
                transactionData.getSide(), transactionData.getPrice(), transactionData.getExecuted(),
                transactionData.getAmount(), transactionData.getFee(),
                ProcessFileV2.log.getName() + " - Process File",
                portfolio
        );
    }

}
