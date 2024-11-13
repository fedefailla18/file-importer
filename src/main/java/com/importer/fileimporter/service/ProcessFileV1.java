package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.dto.TransactionData;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.importer.fileimporter.utils.OperationUtils.SYMBOL;

@Service
@Slf4j
public class ProcessFileV1 extends ProcessFile {

    private final FileImporterService fileImporterService;
    private final TransactionService transactionService;
    private final SymbolService symbolService;

    public ProcessFileV1(FileImporterService fileImporterService,
                         FileImporterService fileImporterService1,
                         TransactionService transactionService,
                         SymbolService symbolService,
                         TransactionAdapterFactory transactionAdapterFactory) {
        super(fileImporterService, transactionAdapterFactory);
        this.fileImporterService = fileImporterService1;
        this.transactionService = transactionService;
        this.symbolService = symbolService;
    }

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
            TransactionData transactionData = getAdapter(row, "Binance");
            String symbol = transactionData.getSymbol();
            if (symbol.isEmpty()) return;

            try {
                processTransactionRow(transactionDetails, transactionData);
            } catch (Exception e) {
                log.error("Error processing row: {}", e.getMessage(), e);
            }
        };
    }

    private void processTransactionRow(Map<String, CoinInformationResponse> transactionDetails,
                                       TransactionData transactionData) {
        String symbol = transactionData.getSymbol();
        boolean isBuy = OperationUtils.isBuy(transactionData.getSide());

        transactionDetails.computeIfAbsent(symbol, k -> createNewCoinInfo(symbol));
        CoinInformationResponse coinInfo = transactionDetails.get(symbol);

        updateCoinInfo(coinInfo, transactionData, isBuy);
        saveTransaction(transactionData);
    }

    private void saveTransaction(TransactionData transactionData) {
        transactionService.saveTransaction(
                transactionData.getCoinName(), transactionData.getPaidWith(), transactionData.getDate(), transactionData.getPair(),
                transactionData.getSide(), transactionData.getPrice(), transactionData.getExecuted(),
                transactionData.getAmount(), transactionData.getFee(), "Process File V1");
    }
}
