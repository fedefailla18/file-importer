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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.importer.fileimporter.utils.OperationUtils.USDT;

@Service
@Slf4j
public class ProcessFileV2 extends ProcessFile {

    private final PortfolioService portfolioService;
    private final TransactionProcessor transactionProcessor;
    private final CoinInformationService coinInformationService;
    private final TransactionService transactionService;
    private final HistoricalPriceCacheService historicalPriceCacheService;
    private final PriceHistoryService priceHistoryService;

    public ProcessFileV2(PortfolioService portfolioService,
                         FileImporterService fileImporterService,
                         TransactionAdapterFactory transactionAdapterFactory,
                         TransactionProcessor transactionProcessor,
                         CoinInformationService coinInformationService,
                         TransactionService transactionService,
                         HistoricalPriceCacheService historicalPriceCacheService,
                         PriceHistoryService priceHistoryService) {
        super(fileImporterService, transactionAdapterFactory);
        this.portfolioService = portfolioService;
        this.transactionProcessor = transactionProcessor;
        this.coinInformationService = coinInformationService;
        this.transactionService = transactionService;
        this.historicalPriceCacheService = historicalPriceCacheService;
        this.priceHistoryService = priceHistoryService;
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols, String portfolioName, String fileType) throws IOException {
        List<Map<?, ?>> rows = getRows(file);
        Portfolio portfolio = portfolioService.findOrSave(portfolioName);
        log.info("Processing {} rows for portfolio: {}", rows.size(), portfolio.getName());

        if (rows != null && !rows.isEmpty()) {
            warmPriceCache(rows, fileType);
        }

        Set<String> processedSymbols = new HashSet<>();

        if (rows != null) {
            rows.forEach(row -> {
                try {
                    TransactionData transactionData = getAdapter(row, fileType);
                    String symbol = transactionData.getSymbol();
                    if (symbol != null && !symbol.isEmpty()) {
                        Transaction transaction = mapToTransaction(transactionData, portfolio);
                        transaction.setFeeSymbol(transactionData.getFeeSymbol());
                        transactionProcessor.process(transaction);
                        processedSymbols.add(symbol);
                    }
                } catch (Exception e) {
                    log.error("[ERROR_LOG] Error processing row: {}", e.getMessage(), e);
                }
            });
        }

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

    private void warmPriceCache(List<Map<?, ?>> rows, String fileType) {
        Map<String, List<LocalDateTime>> symbolToHours = rows.stream()
            .map(row -> {
                try { return getAdapter(row, fileType); } catch (Exception e) { return null; }
            })
            .filter(Objects::nonNull)
            .filter(td -> td.getSymbol() != null && !td.getSymbol().isEmpty())
            .collect(Collectors.groupingBy(
                td -> td.getSymbol().toUpperCase(),
                Collectors.mapping(
                    td -> DateUtils.getLocalDateTime(td.getDate()).truncatedTo(ChronoUnit.HOURS),
                    Collectors.toList())));

        Map<String, BigDecimal> warmupPrices = new HashMap<>();
        symbolToHours.forEach((sym, hours) -> {
            List<LocalDateTime> distinct = hours.stream().distinct().collect(Collectors.toList());
            warmupPrices.putAll(priceHistoryService.findAllForWarmup(sym, USDT, distinct));
        });

        warmupPrices.forEach((key, price) -> {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                historicalPriceCacheService.put(parts[0], parts[1], LocalDateTime.parse(parts[2]), price);
            }
        });

        log.info("Pre-warmed Redis price cache with {} entries for {} symbols",
                warmupPrices.size(), symbolToHours.size());
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
