package com.importer.fileimporter.service.usecase;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class CalculateAmountSpent {

    private final PricingFacade pricingFacade;

    public BigDecimal execute(String symbol, List<Transaction> transactions, CoinInformationResponse response) {
        return transactions.parallelStream()
                .map(transaction -> getAmountSpentInUsdtPerTransaction(symbol, transaction, response))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * This method not only gets the payed amount in USDT per transaction but also keep track of other fields such as:
     * spent: this field keeps the amount spent and since it's a key value pair, helps to keep track of the spent in each currency.
     * stableTotalCost: the total amount paid historically for this coin
     * totalRealizedProfit: the total amount sold historically. This is the gross usdt
     *
     * @param symbol
     * @param transaction
     * @param response
     * @return
     */
    public BigDecimal getAmountSpentInUsdtPerTransaction(String symbol, Transaction transaction, CoinInformationResponse response) {
        String payedWithSymbol = transaction.getPayedWith();
        BigDecimal payedAmount = transaction.getPayedAmount();
        BigDecimal executed = transaction.getTransactionId().getExecuted();
        boolean isBuy = OperationUtils.isBuy(transaction.getTransactionId().getSide());

        if (!OperationUtils.isStable(payedWithSymbol)) {
            BigDecimal priceInStable = getPayedAmountInStable(symbol, transaction.getTransactionId().getDateUtc());
            payedAmount = priceInStable.multiply(executed);
        }

        if (isBuy) {
            BigDecimal finalPayedAmount = payedAmount;
            response.getSpent().computeIfPresent(payedWithSymbol, (k, v) -> finalPayedAmount.add(v));
            response.getSpent().computeIfAbsent(payedWithSymbol, k -> finalPayedAmount);
            response.setStableTotalCost(response.getStableTotalCost().add(payedAmount));
            return payedAmount;
        } else {
            // Calculating profit for the sale
//            BigDecimal profitFromSale = executed.multiply(priceInStable).subtract(payedAmount); // TODO: This is wrong. executed*price is transaction.payedAmount
//            BigDecimal realizedProfit = response.getRealizedProfit() == null ? BigDecimal.ZERO : response.getRealizedProfit();
//            response.setRealizedProfit(realizedProfit.add(profitFromSale).setScale(5, RoundingMode.UP));
            response.addTotalRealizedProfit(payedAmount);
            return payedAmount.negate();
        }
    }

    private BigDecimal getPayedAmountInStable(String symbol, LocalDateTime dateUtc) {
        return pricingFacade.getPriceInUsdt(symbol, dateUtc);
    }

    public BigDecimal execute(Transaction transaction, CoinInformationResponse response) {
        return getAmountSpentInUsdtPerTransaction(transaction.getSymbol(), transaction, response);
    }
}
