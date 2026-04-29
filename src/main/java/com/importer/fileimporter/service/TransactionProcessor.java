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
    private final PortfolioService portfolioService;

    @Transactional
    public Transaction process(Transaction transaction) {
        log.info("Processing transaction: {} {} {} @ {} for portfolio {}", 
                transaction.getSide(), transaction.getExecuted(), transaction.getSymbol(), transaction.getPrice(),
                transaction.getPortfolio() != null ? transaction.getPortfolio().getName() : "NULL");
        
        // 1. Save the transaction if not already saved
        if (transaction.getId() == null) {
            transaction = transactionService.save(transaction);
        }

        Portfolio portfolio = transaction.getPortfolio();
        if (portfolio == null) {
            log.warn("Transaction has no portfolio, skipping holding updates.");
            return transaction;
        }
        
        // Ensure portfolio is attached to current persistence context
        portfolio = portfolioService.findOrSave(portfolio.getName());

        log.info("Processing symbol {} for portfolio {} (UUID: {})", transaction.getSymbol(), portfolio.getName(), portfolio.getId());

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
        String side = transaction.getSide();
        boolean isIncrease = OperationUtils.isBuy(side) || OperationUtils.isDeposit(side);
        boolean isDecrease = OperationUtils.isSell(side) || OperationUtils.isWithdraw(side);
        
        BigDecimal executed = transaction.getExecuted();
        BigDecimal priceInUsdt = getPriceInUsdt(transaction);

        Holding holding = holdingService.getHolding(portfolio, symbol);
        
        BigDecimal oldAmount = holding.getAmount() != null ? holding.getAmount() : BigDecimal.ZERO;
        BigDecimal oldCostBasis = holding.getStableTotalCost() != null ? holding.getStableTotalCost() : BigDecimal.ZERO;

        if (isIncrease) {
            // BUY or DEPOSIT: Increase amount and cost basis
            holding.setAmount(oldAmount.add(executed));
            holding.setTotalAmountBought(safeAdd(holding.getTotalAmountBought(), executed));

            BigDecimal costInUsdt = executed.multiply(priceInUsdt);
            BigDecimal stableFee = getStableFeeAmount(transaction);
            holding.setStableTotalCost(oldCostBasis.add(costInUsdt).add(stableFee));
        } else if (isDecrease) {
            // SELL or WITHDRAW: Decrease amount and cost basis proportionally, calculate realized profit
            BigDecimal amountToRemove = executed.min(oldAmount);
            if (executed.compareTo(oldAmount) > 0) {
                log.warn("{} more than held amount for {}: {} > {}", side, symbol, executed, oldAmount);
            }

            holding.setAmount(oldAmount.subtract(amountToRemove));
            holding.setTotalAmountSold(safeAdd(holding.getTotalAmountSold(), executed));

            if (oldAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Average cost per unit
                BigDecimal avgCost = oldCostBasis.divide(oldAmount, 10, RoundingMode.HALF_UP);
                BigDecimal costOfRemovedUnits = avgCost.multiply(amountToRemove);
                
                // Realized Profit = (Current Price - Avg Cost) * amount
                BigDecimal exitValue = amountToRemove.multiply(priceInUsdt);
                BigDecimal profit = exitValue.subtract(costOfRemovedUnits);
                
                holding.setTotalRealizedProfitUsdt(safeAdd(holding.getTotalRealizedProfitUsdt(), profit));
                holding.setStableTotalCost(oldCostBasis.subtract(costOfRemovedUnits).max(BigDecimal.ZERO));
            }
        }
        
        holding.setModified(LocalDateTime.now());
        holding.setModifiedBy("TransactionProcessor");
        holdingService.save(holding);
        transactionService.flush(); 
    }

    private void updatePaidWithHolding(Transaction transaction, Portfolio portfolio) {
        String paidWith = transaction.getPaidWith();
        if (OperationUtils.isStable(paidWith)) {
            return; 
        }

        boolean isBuy = OperationUtils.isBuy(transaction.getSide());
        BigDecimal paidAmount = transaction.getPaidAmount();
        
        Holding holding = holdingService.getHolding(portfolio, paidWith);
        BigDecimal oldAmount = holding.getAmount() != null ? holding.getAmount() : BigDecimal.ZERO;
        BigDecimal oldCostBasis = holding.getStableTotalCost() != null ? holding.getStableTotalCost() : BigDecimal.ZERO;

        if (isBuy) {
            // It's a SELL for the paidWith currency
            BigDecimal amountToSell = paidAmount.min(oldAmount);
            if (paidAmount.compareTo(oldAmount) > 0) {
                log.warn("Selling more than held amount for {}: {} > {}", paidWith, paidAmount, oldAmount);
            }
            holding.setAmount(oldAmount.subtract(amountToSell));
            holding.setTotalAmountSold(safeAdd(holding.getTotalAmountSold(), paidAmount));
            
            if (oldAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgCost = oldCostBasis.divide(oldAmount, 10, RoundingMode.HALF_UP);
                BigDecimal costOfSoldUnits = avgCost.multiply(amountToSell);
                
                BigDecimal priceOfPaidWithInUsdt = pricingFacade.getPriceInUsdt(paidWith, transaction.getDateUtc());
                BigDecimal saleValue = amountToSell.multiply(priceOfPaidWithInUsdt);
                BigDecimal profit = saleValue.subtract(costOfSoldUnits);
                
                holding.setTotalRealizedProfitUsdt(safeAdd(holding.getTotalRealizedProfitUsdt(), profit));
                holding.setStableTotalCost(oldCostBasis.subtract(costOfSoldUnits).max(BigDecimal.ZERO));
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
        transactionService.flush();
    }

    private BigDecimal getPriceInUsdt(Transaction transaction) {
        if (OperationUtils.isStable(transaction.getPaidWith())) {
            return transaction.getPrice();
        }
        BigDecimal price = pricingFacade.getPriceInUsdt(transaction.getSymbol(), transaction.getDateUtc());
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return transaction.getPrice(); // Fallback to provided price if it's already in USDT or proxy fails
        }
        return price;
    }

    private BigDecimal getStableFeeAmount(Transaction transaction) {
        BigDecimal feeAmount = transaction.getFeeAmount();
        String feeSymbol = transaction.getFeeSymbol();
        if (feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) > 0
                && OperationUtils.isStable(feeSymbol)) {
            return feeAmount;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal safeAdd(BigDecimal current, BigDecimal toAdd) {
        if (current == null) return toAdd;
        if (toAdd == null) return current;
        return current.add(toAdd);
    }
}
