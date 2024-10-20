package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
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
    private final CalculateAmountSpent calculateAmountSpent;
    private final TransactionService transactionService;

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

        CoinInformationResponse response = CoinInformationResponse.createEmpty(symbol);
        calculateAndSetAmountsOnlyInStable(symbol, unprocessedTransactions, response);

        return response;
    }

    private void calculateAndSetAmountsOnlyInStable(String symbol, List<Transaction> transactions,
                                            CoinInformationResponse response) {
        BigDecimal totalHeldAmount = BigDecimal.ZERO;
        BigDecimal totalCostInStable = BigDecimal.ZERO;
        List<Transaction> transactionsSelling = new ArrayList<>();
        Portfolio portfolio = transactions.stream().findFirst()
                .map(Transaction::getPortfolio).orElse(null);

        for (Transaction transaction : transactions) {
            log.info("··· PROCESSING {}", transaction.getSymbol());

            BigDecimal executed = transaction.getExecuted();
            boolean isBuy = OperationUtils.isBuy(transaction.getSide());

            if (isBuy) {
                totalHeldAmount = totalHeldAmount.add(executed);
                BigDecimal paidAmountInStable = calculateAmountSpent.getAmountSpentInUsdt(transaction, response, portfolio);// here we are setting spent
                totalCostInStable = totalCostInStable.add(paidAmountInStable);
                response.addTotalAmountBought(executed, transaction.getSide());
            } else {
                transactionsSelling.add(transaction);
            }
            markTransactionProcessed(transaction);
        }

        BigDecimal realizedProfit = BigDecimal.ZERO;
        for (Transaction transaction : transactionsSelling) {
            BigDecimal executed = transaction.getExecuted();

            BigDecimal amountSold = executed.min(totalHeldAmount); // Ensure not selling more than held
            if (executed.compareTo(totalHeldAmount) > 0 || amountSold.compareTo(BigDecimal.ZERO) > 0) {
                log.warn(String.format("Executed is greater than totalHeldAmount. Why? Transaction: %s", transaction));
            }

            totalHeldAmount = totalHeldAmount.subtract(amountSold);
            response.setAmount(totalHeldAmount);

            BigDecimal amountSpentInUsdt = calculateAmountSpent.getAmountSpentInUsdt(transaction, response, portfolio);
            realizedProfit = realizedProfit.subtract(amountSpentInUsdt);
            totalCostInStable = totalCostInStable.subtract(amountSpentInUsdt);
            response.addTotalAmountSold(executed, transaction.getSide());
        }

        response.setRealizedProfit(realizedProfit);
        response.setAmount(totalHeldAmount);

        BigDecimal currentMarketPrice = pricingFacade.getCurrentMarketPrice(symbol);
        response.setCurrentPrice(currentMarketPrice);

        BigDecimal currentMarketValue = currentMarketPrice.multiply(totalHeldAmount);
        response.setCurrentPositionInUsdt(currentMarketValue);

        // TODO: is this the same? what
        response.setUnrealizedProfit(currentMarketValue);
        response.setUnrealizedTotalProfitMinusTotalCost(currentMarketValue.subtract(totalCostInStable));

        setAndSaveHolding(symbol, response, portfolio);
    }

    private void markTransactionProcessed(Transaction transaction) {
        transaction.setProcessed(true);
        LocalDateTime now = LocalDateTime.now();
        transaction.setLastProcessedAt(now);
        transaction.setModified(now);
        transaction.setModifiedBy(this.getClass().getName());
        transactionService.save(transaction);
    }

    private void setAndSaveHolding(String symbol, CoinInformationResponse response, Portfolio portfolio) {
        Holding holding = holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol);
        updateHoldingFields(holding, response);
        holdingService.save(holding);
    }

    private void updateHoldingFields(Holding holding, CoinInformationResponse response) {
        // Null-safe updates for all fields
        holding.setAmount(OperationUtils.sumBigDecimal(holding.getAmount(), response.getAmount()));
        holding.setTotalAmountBought(OperationUtils.sumBigDecimal(holding.getTotalAmountBought(), response.getTotalAmountBought()));
        holding.setTotalAmountSold(OperationUtils.sumBigDecimal(holding.getTotalAmountSold(), response.getTotalAmountSold()));
        holding.setStableTotalCost(OperationUtils.sumBigDecimal(holding.getStableTotalCost(), response.getStableTotalCost()));
        holding.setCurrentPositionInUsdt(OperationUtils.sumBigDecimal(holding.getCurrentPositionInUsdt(), response.getCurrentPositionInUsdt()));
        holding.setTotalRealizedProfitUsdt(OperationUtils.sumBigDecimal(holding.getTotalRealizedProfitUsdt(), response.getTotalRealizedProfitUsdt()));
        holding.setAmountInUsdt(OperationUtils.sumBigDecimal(holding.getAmountInUsdt(), response.getUnrealizedProfit()));
    }

}
