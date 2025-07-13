package com.importer.fileimporter.service.usecase;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.service.HoldingService;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class CalculateAmountSpent {

    private final PricingFacade pricingFacade;
    private final HoldingService holdingService;

    /**
     * When transaction is sell I'm returning a negative number.
     * This method not only gets the paid amount in USDT per transaction but also keep track of other fields such as:
     * spent: this field keeps the amount spent and since it's a key value pair, helps to keep track of the spent in each currency.
     * stableTotalCost: the total amount paid historically for this coin
     * totalRealizedProfit: the total amount sold historically. This is the gross usdt
     *
     * @param transaction The transaction to calculate the amount spent for
     * @param response The CoinInformationResponse object to update with the calculated values
     * @param portfolio The portfolio associated with the transaction, can be null if no portfolio update is needed
     * @return The amount spent in USDT (positive for buy, negative for sell)
     */
    public BigDecimal getAmountSpentInUsdt(Transaction transaction, CoinInformationResponse response, Portfolio portfolio) {
        String symbol = transaction.getSymbol();
        String paidWithSymbol = transaction.getPaidWith();
        BigDecimal paidAmount = transaction.getPaidAmount();
        BigDecimal executed = transaction.getExecuted();
        boolean isBuy = OperationUtils.isBuy(transaction.getSide());


        if (paidAmount == null || executed == null) {
            log.warn("Paid amount or executed amount is null for transaction: {}", transaction);
            return BigDecimal.ZERO;
        }

        // Only add the spent for the original transaction if it's a buy transaction
        if (isBuy) {
            response.addSpent(paidWithSymbol, paidAmount);
        } else {
            response.addSold(paidWithSymbol, paidAmount);
        }

        BigDecimal priceInStable;
        if (!OperationUtils.isStable(paidWithSymbol)) {
            priceInStable = getPriceInStable(symbol, transaction.getDateUtc());
            paidAmount = priceInStable.multiply(executed);
            if (portfolio != null) {
                updatePaidWithHolding(!isBuy, paidWithSymbol, transaction.getPaidAmount(), portfolio, executed, paidAmount);
            }
        }

        if (isBuy) {
            response.setStableTotalCost(response.getStableTotalCost().add(paidAmount));
            return paidAmount;
        } else {
            // For sell transactions, we add the amount to the total realized profit
            // and return the negative of the paid amount (since it's money coming in, not going out)
            response.addTotalRealizedProfit(paidAmount);
            return paidAmount.negate();
        }
    }

    private BigDecimal getPriceInStable(String symbol, LocalDateTime dateUtc) {
        return pricingFacade.getPriceInUsdt(symbol, dateUtc);
    }

    private void updatePaidWithHolding(boolean isBuy, String paidWithSymbol, BigDecimal paidAmount, Portfolio portfolio, BigDecimal executed, BigDecimal paidInStable) {
        holdingService.updatePaidWithHolding(isBuy, paidWithSymbol, paidAmount, portfolio, executed, paidInStable);
    }

}
