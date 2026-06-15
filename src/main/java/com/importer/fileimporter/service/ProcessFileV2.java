package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.dto.TransactionData;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessFileV2 extends ProcessFile {

    private final PortfolioService portfolioService;
    private final CoinInformationService coinInformationService;
    private final TransactionService transactionService;

    public ProcessFileV2(PortfolioService portfolioService,
                         FileImporterService fileImporterService,
                         TransactionAdapterFactory transactionAdapterFactory,
                         CoinInformationService coinInformationService,
                         TransactionService transactionService) {
        super(fileImporterService, transactionAdapterFactory);
        this.portfolioService = portfolioService;
        this.coinInformationService = coinInformationService;
        this.transactionService = transactionService;
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols, String portfolioName) throws IOException {
        List<Map<?, ?>> rows = getRows(file);
        Portfolio portfolio = portfolioService.findOrSave(portfolioName);
        log.info("Processing {} rows for portfolio: {}", rows.size(), portfolio.getName());

        Set<String> processedSymbols = new HashSet<>();

        rows.forEach(row -> {
            try {
                TransactionData transactionData = getAdapter(row, portfolio.getName());
                String symbol = transactionData.getSymbol();
                if (symbol != null && !symbol.isEmpty()) {
                    Transaction transaction = mapToTransaction(transactionData, portfolio);
                    transactionService.save(transaction);
                    processedSymbols.add(symbol);
                }
            } catch (Exception e) {
                log.error("[ERROR_LOG] Error processing row: {}", e.getMessage(), e);
            }
        });

        List<CoinInformationResponse> coinInfos = processedSymbols.stream()
                .map(symbol -> coinInformationService.getCoinInformationResponse(symbol, transactionService.findByPortfolioAndSymbol(portfolio, symbol)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return FileInformationResponse.builder()
                .portfolio(portfolioName)
                .amount(rows.size())
                .coinInformationResponse(coinInfos)
                .build();
    }

    private Transaction mapToTransaction(TransactionData data, Portfolio portfolio) {
        return Transaction.builder()
                .dateUtc(DateUtils.getLocalDateTime(data.getDate()))
                .pair(data.getPair())
                .executed(data.getExecuted())
                .side(data.getSide())
                .price(data.getPrice())
                .symbol(data.getSymbol())
                .paidWith(data.getPaidWith())
                .paidAmount(data.getAmount())
                .feeAmount(data.getFee())
                .created(LocalDateTime.now())
                .createdBy("ProcessFileV2")
                .portfolio(portfolio)
                .build();
    }
}
