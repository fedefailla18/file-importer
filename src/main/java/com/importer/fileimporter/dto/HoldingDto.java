package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing a cryptocurrency holding in a portfolio.
 * This class is used to transfer holding data between the backend and frontend.
 * It contains all the necessary information about a cryptocurrency holding,
 * including its amount, value in different currencies, and transaction history metrics.
 */
@Data
@Builder
public class HoldingDto {
    /**
     * The cryptocurrency symbol (e.g., BTC, ETH).
     */
    private String symbol;

    /**
     * The name of the portfolio this holding belongs to.
     */
    private String portfolioName;

    /**
     * The amount of cryptocurrency held in its native units.
     */
    private BigDecimal amount;

    /**
     * The value of the holding converted to BTC.
     * Calculated as amount * priceInBtc.
     */
    private BigDecimal amountInBtc;

    /**
     * The value of the holding converted to USDT.
     * Calculated as amount * priceInUsdt.
     */
    private BigDecimal amountInUsdt;

    /**
     * The current price of one unit of the cryptocurrency in BTC.
     */
    private BigDecimal priceInBtc;

    /**
     * The current price of one unit of the cryptocurrency in USDT.
     */
    private BigDecimal priceInUsdt;

    /**
     * The percentage this holding represents in the portfolio.
     * Calculated as (amountInUsdt / portfolio total value in USDT) * 100.
     */
    private BigDecimal percentage;

    /**
     * The total amount of this cryptocurrency that has been bought across all transactions.
     */
    private BigDecimal totalAmountBought;

    /**
     * The total amount of this cryptocurrency that has been sold across all transactions.
     */
    private BigDecimal totalAmountSold;

    /**
     * The total cost in stable currency (USDT) spent to acquire this holding.
     * This represents the historical cost basis.
     */
    private BigDecimal inventoryCostUsdt;

    /**
     * The current market value of the holding in USDT.
     * This is calculated based on the current market price.
     */
    private BigDecimal currentPositionInUsdt;

    /**
     * The total profit realized from selling this cryptocurrency, in USDT.
     * This is the sum of all sell transaction values minus their cost basis.
     */
    private BigDecimal totalRealizedProfitUsdt;
}
