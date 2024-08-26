package com.importer.fileimporter.facade;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.HoldingService;
import com.importer.fileimporter.service.PortfolioService;
import com.importer.fileimporter.service.TransactionService;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class CoinInformationFacade {

    private final CalculateAmountSpent calculateAmountSpent;
    private final TransactionService transactionService;
    private final PricingFacade pricingFacade;
    private final HoldingService holdingService;
    private final PortfolioService portfolioService;

    public List<CoinInformationResponse> getTransactionsInformation() {
        return getTransactionsInformation();
    }

    public List<CoinInformationResponse> getPortfolioTransactionsInformation(String portfolio) {
        Portfolio byName = portfolioService.getByName(portfolio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found."));
        List<Transaction> transactions = transactionService.findByPortfolio(byName);
        Map<String, List<Transaction>> collect = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getSymbol));
        return collect.entrySet().stream()
                .map(e -> getCoinInformationResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public CoinInformationResponse getTransactionsInformationBySymbol(String symbol) {
        List<Transaction> transactions = transactionService.getAllBySymbol(symbol);
        return getCoinInformationResponse(symbol, transactions);
    }

    private CoinInformationResponse getCoinInformationResponse(String symbol, List<Transaction> transactions) {
        if (CollectionUtils.isEmpty(transactions)) {
            log.warn("No transactions found for symbol: {}", symbol);
            return CoinInformationResponse.createEmpty(symbol);
        }

        CoinInformationResponse response = CoinInformationResponse.createEmpty(symbol);
        calculateAndSetAmountsOnlyInStable(symbol, transactions, response);

        return response;
    }

    void calculateAndSetAmountsOnlyInStable(String symbol, List<Transaction> transactions,
                                                  CoinInformationResponse response) {
        BigDecimal totalHeldAmount = BigDecimal.ZERO;
        BigDecimal totalCostInStable = BigDecimal.ZERO;
        List<Transaction> transactionsSelling = new ArrayList<>();

        for (Transaction transaction : transactions) {
            BigDecimal executed = transaction.getTransactionId().getExecuted();
            boolean isBuy = OperationUtils.isBuy(transaction.getTransactionId().getSide());

            if (isBuy) {
                totalHeldAmount = totalHeldAmount.add(executed);
                BigDecimal paidAmountInStable = calculateAmountSpent.getAmountSpentInUsdt(transaction, response);// here we are setting spent
                totalCostInStable = totalCostInStable.add(paidAmountInStable);
                response.addTotalAmountBought(executed, transaction.getTransactionId().getSide());
            } else {
                transactionsSelling.add(transaction);
            }
        }

        BigDecimal realizedProfit = BigDecimal.ZERO;
        for (Transaction transaction : transactionsSelling) {
            BigDecimal executed = transaction.getTransactionId().getExecuted();

            BigDecimal amountSold = executed.min(totalHeldAmount); // Ensure not selling more than held
            if (executed.compareTo(totalHeldAmount) > 0 || amountSold.compareTo(BigDecimal.ZERO) > 0) {
                log.warn(String.format("Executed is greater than totalHeldAmount. Why? Transaction: %s", transaction));
            }

            totalHeldAmount = totalHeldAmount.subtract(amountSold);
            response.setAmount(totalHeldAmount);

            BigDecimal amountSpentInUsdt = calculateAmountSpent.getAmountSpentInUsdt(transaction, response);
            realizedProfit = realizedProfit.subtract(amountSpentInUsdt);
            totalCostInStable = totalCostInStable.subtract(amountSpentInUsdt);
            response.addTotalAmountSold(executed, transaction.getTransactionId().getSide());
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

        setAndSaveHolding(symbol, response);
    }

    private void setAndSaveHolding(String symbol, CoinInformationResponse response) {
        Portfolio portfolio = portfolioService.getByName("Binance")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found."));
        Holding holding = holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol);
        holding.setAmount(response.getAmount());
        holding.setTotalAmountBought(response.getTotalAmountBought());
        holding.setTotalAmountSold(response.getTotalAmountSold());
        holding.setStableTotalCost(response.getStableTotalCost());
        holding.setCurrentPositionInUsdt(response.getCurrentPositionInUsdt());
        holding.setTotalRealizedProfitUsdt(response.getTotalRealizedProfitUsdt());
        holding.setAmountInUsdt(response.getUnrealizedProfit());
        holdingService.save(holding);
    }
}
