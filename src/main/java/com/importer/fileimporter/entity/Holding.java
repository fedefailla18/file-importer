package com.importer.fileimporter.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a cryptocurrency holding in a portfolio.
 * A holding tracks the amount of a specific cryptocurrency owned in a portfolio,
 * along with various metrics related to its value and transaction history.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio_holding")
public class Holding {

    /**
     * Unique identifier for the holding.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    private UUID id;

    /**
     * The portfolio this holding belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    /**
     * The cryptocurrency symbol (e.g., BTC, ETH).
     */
    private String symbol;

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
    private BigDecimal percent;

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
     * - “How much stable/fiat is currently invested in the units I still hold?”
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

    /**
     * The timestamp when this holding record was created.
     */
    private LocalDateTime created;

    /**
     * The identifier of who created this holding record.
     */
    private String createdBy;

    /**
     * The timestamp when this holding record was last modified.
     */
    private LocalDateTime modified;

    /**
     * The identifier of who last modified this holding record.
     */
    private String modifiedBy;

}
