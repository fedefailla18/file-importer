package com.importer.fileimporter.facade;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.TransactionService;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class CoinInformationFacade {

    private final CalculateAmountSpent calculateAmountSpent;
    private final TransactionService transactionService;
    private final PricingFacade pricingFacade;

    public CoinInformationResponse getTransactionsInformation(String symbol) {
        List<Transaction> transactions = transactionService.getAllBySymbol(symbol);
        if (CollectionUtils.isEmpty(transactions)) {
            log.warn("No transactions found for symbol: {}", symbol);
            return CoinInformationResponse.createEmpty(symbol);
        }

        CoinInformationResponse response = CoinInformationResponse.createEmpty(symbol);
        calculateAndSetAmountsOnlyInStable(transactions, response);

        return response;
    }

    BigDecimal calculateAndSetAmountsOnlyInStable(List<Transaction> transactions, CoinInformationResponse response) {
        BigDecimal totalHeldAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<Transaction> transactionsSelling = new ArrayList<>();

        for (Transaction transaction : transactions) {
            BigDecimal executed = transaction.getTransactionId().getExecuted();
            boolean isBuy = OperationUtils.isBuy(transaction.getTransactionId().getSide());

            if (isBuy) {
                totalHeldAmount = totalHeldAmount.add(executed);
                BigDecimal paidAmountInStable = calculateAmountSpent.getAmountSpentInUsdt(transaction, response);// here we are setting spent
                totalCost = totalCost.add(paidAmountInStable);
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
            totalCost = totalCost.subtract(amountSpentInUsdt);
            response.addTotalAmountSold(executed, transaction.getTransactionId().getSide());
        }

        response.setRealizedProfit(realizedProfit);
        response.setAmount(totalHeldAmount);

        BigDecimal currentMarketPrice = pricingFacade.getCurrentMarketPrice(transactions.get(0).getSymbol());
        response.setCurrentPrice(currentMarketPrice);

        BigDecimal currentMarketValue = currentMarketPrice.multiply(totalHeldAmount);
        response.setCurrentPositionInUsdt(currentMarketValue);

        // TODO: is this the same? what
        response.setUnrealizedProfit(currentMarketValue);
        response.setUnrealizedTotalProfitMinusTotalCost(currentMarketValue.subtract(totalCost));

        return totalHeldAmount;
    }
}
