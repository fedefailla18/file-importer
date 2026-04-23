package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class CoinInformationService {

    private final PricingFacade pricingFacade;
    private final HoldingService holdingService;
    private final TransactionProcessor transactionProcessor;

    public CoinInformationResponse getCoinInformationResponse(String symbol, List<Transaction> transactions) {
        if (CollectionUtils.isEmpty(transactions)) {
            log.warn("No transactions found for symbol: {}", symbol);
            return CoinInformationResponse.createEmpty(symbol);
        }

        List<Transaction> unprocessedTransactions = transactions.stream()
                .filter(transaction -> !transaction.isProcessed())
                .collect(Collectors.toList());
        if (unprocessedTransactions.isEmpty()) {
            log.warn("No unprocessed transactions found for symbol: {}", symbol);
            return null;
        }

        unprocessedTransactions.forEach(transactionProcessor::process);

        // Fetch the updated holding to build the response
        Portfolio portfolio = unprocessedTransactions.get(0).getPortfolio();
        Holding holding = holdingService.getHolding(portfolio, symbol);

        BigDecimal currentMarketPrice = pricingFacade.getCurrentMarketPrice(symbol);
        BigDecimal amount = holding.getAmount() != null ? holding.getAmount() : BigDecimal.ZERO;
        BigDecimal currentMarketValue = currentMarketPrice.multiply(amount);

        BigDecimal stableTotalCost = holding.getStableTotalCost() != null ? holding.getStableTotalCost() : BigDecimal.ZERO;

        return CoinInformationResponse.builder()
                .coinName(symbol)
                .amount(amount)
                .totalAmountBought(holding.getTotalAmountBought())
                .totalAmountSold(holding.getTotalAmountSold())
                .stableTotalCost(stableTotalCost)
                .currentPrice(currentMarketPrice)
                .currentPositionInUsdt(currentMarketValue)
                .totalRealizedProfitUsdt(holding.getTotalRealizedProfitUsdt())
                .unrealizedProfit(currentMarketValue.subtract(stableTotalCost))
                .build();
    }

}
