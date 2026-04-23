package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final TransactionService transactionService;
    private final HoldingService holdingService;
    private final PricingFacade pricingFacade;

    @Transactional
    public Transaction process(Transaction transaction) {
        log.info("Processing transaction: {} {} {} @ {}", 
                transaction.getSide(), transaction.getExecuted(), transaction.getSymbol(), transaction.getPrice());
        
        // 1. Save the transaction if not already saved
        if (transaction.getId() == null) {
            transaction = transactionService.save(transaction);
        }

        Portfolio portfolio = transaction.getPortfolio();
        if (portfolio == null) {
            log.warn("Transaction has no portfolio, skipping holding updates.");
            return transaction;
        }

        // 2. Update Holding for the primary symbol
        updatePrimaryHolding(transaction, portfolio);

        // 3. Update Holding for the "paid with" symbol (if not stable)
        updatePaidWithHolding(transaction, portfolio);

        // 4. Mark as processed
        transaction.setProcessed(true);
        transaction.setLastProcessedAt(LocalDateTime.now());
        return transactionService.save(transaction);
    }

    private void updatePrimaryHolding(Transaction transaction, Portfolio portfolio) {
        String symbol = transaction.getSymbol();
        boolean isBuy = OperationUtils.isBuy(transaction.getSide());
        BigDecimal executed = transaction.getExecuted();
        BigDecimal priceInUsdt = getPriceInUsdt(transaction);

        Holding holding = holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol);
        
        BigDecimal oldAmount = holding.getAmount() != null ? holding.getAmount() : BigDecimal.ZERO;
        BigDecimal oldCostBasis = holding.getStableTotalCost() != null ? holding.getStableTotalCost() : BigDecimal.ZERO;

        if (isBuy) {
            // BUY: Increase amount and cost basis
            holding.setAmount(oldAmount.add(executed));
            holding.setTotalAmountBought(safeAdd(holding.getTotalAmountBought(), executed));
            
            BigDecimal costInUsdt = executed.multiply(priceInUsdt);
            holding.setStableTotalCost(oldCostBasis.add(costInUsdt));
        } else {
            // SELL: Decrease amount and cost basis proportionally, calculate realized profit
            BigDecimal amountToSell = executed.min(oldAmount);
            if (executed.compareTo(oldAmount) > 0) {
                log.warn("Selling more than held amount for {}: {} > {}", symbol, executed, oldAmount);
            }

            holding.setAmount(oldAmount.subtract(amountToSell));
            holding.setTotalAmountSold(safeAdd(holding.getTotalAmountSold(), executed));

            if (oldAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Average cost per unit
                BigDecimal avgCost = oldCostBasis.divide(oldAmount, 10, RoundingMode.HALF_UP);
                BigDecimal costOfSoldUnits = avgCost.multiply(amountToSell);
                
                // Realized Profit = (Sale Price - Avg Cost) * amount
                BigDecimal saleValue = amountToSell.multiply(priceInUsdt);
                BigDecimal profit = saleValue.subtract(costOfSoldUnits);
                
                holding.setTotalRealizedProfitUsdt(safeAdd(holding.getTotalRealizedProfitUsdt(), profit));
                holding.setStableTotalCost(oldCostBasis.subtract(costOfSoldUnits));
            }
        }
        
        holding.setModified(LocalDateTime.now());
        holding.setModifiedBy("TransactionProcessor");
        holdingService.save(holding);
    }

    private void updatePaidWithHolding(Transaction transaction, Portfolio portfolio) {
        String paidWith = transaction.getPaidWith();
        if (OperationUtils.isStable(paidWith)) {
            return; // No need to track holdings for stable coins usually, or handle separately if needed
        }

        boolean isBuy = OperationUtils.isBuy(transaction.getSide());
        BigDecimal paidAmount = transaction.getPaidAmount();
        
        // When we BUY BTC with ETH:
        // BTC holding increases (handled in updatePrimaryHolding)
        // ETH holding decreases (SELL ETH)
        
        // When we SELL BTC for ETH:
        // BTC holding decreases (handled in updatePrimaryHolding)
        // ETH holding increases (BUY ETH)
        
        Holding holding = holdingService.getOrCreateByPortfolioAndSymbol(portfolio, paidWith);
        BigDecimal oldAmount = holding.getAmount() != null ? holding.getAmount() : BigDecimal.ZERO;
        BigDecimal oldCostBasis = holding.getStableTotalCost() != null ? holding.getStableTotalCost() : BigDecimal.ZERO;

        // The "side" for the paidWith currency is the opposite of the transaction side
        if (isBuy) {
            // It's a SELL for the paidWith currency
            BigDecimal amountToSell = paidAmount.min(oldAmount);
            holding.setAmount(oldAmount.subtract(amountToSell));
            holding.setTotalAmountSold(safeAdd(holding.getTotalAmountSold(), paidAmount));
            
            if (oldAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgCost = oldCostBasis.divide(oldAmount, 10, RoundingMode.HALF_UP);
                BigDecimal costOfSoldUnits = avgCost.multiply(amountToSell);
                
                BigDecimal priceOfPaidWithInUsdt = pricingFacade.getPriceInUsdt(paidWith, transaction.getDateUtc());
                BigDecimal saleValue = amountToSell.multiply(priceOfPaidWithInUsdt);
                BigDecimal profit = saleValue.subtract(costOfSoldUnits);
                
                holding.setTotalRealizedProfitUsdt(safeAdd(holding.getTotalRealizedProfitUsdt(), profit));
                holding.setStableTotalCost(oldCostBasis.subtract(costOfSoldUnits));
            }
        } else {
            // It's a BUY for the paidWith currency
            holding.setAmount(oldAmount.add(paidAmount));
            holding.setTotalAmountBought(safeAdd(holding.getTotalAmountBought(), paidAmount));
            
            BigDecimal priceOfPaidWithInUsdt = pricingFacade.getPriceInUsdt(paidWith, transaction.getDateUtc());
            BigDecimal costInUsdt = paidAmount.multiply(priceOfPaidWithInUsdt);
            holding.setStableTotalCost(oldCostBasis.add(costInUsdt));
        }

        holding.setModified(LocalDateTime.now());
        holding.setModifiedBy("TransactionProcessor (PaidWith)");
        holdingService.save(holding);
    }

    private BigDecimal getPriceInUsdt(Transaction transaction) {
        if (OperationUtils.isStable(transaction.getPaidWith())) {
            return transaction.getPrice();
        }
        return pricingFacade.getPriceInUsdt(transaction.getSymbol(), transaction.getDateUtc());
    }

    private BigDecimal safeAdd(BigDecimal current, BigDecimal toAdd) {
        if (current == null) return toAdd;
        if (toAdd == null) return current;
        return current.add(toAdd);
    }
}
