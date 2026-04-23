package com.importer.fileimporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.importer.fileimporter.utils.OperationUtils.USDT;
import static java.math.BigDecimal.ZERO;

/**
 * This class keeps the information of a coin.
 * coinName: name of the symbol (e.g. BTC)
 * amount: current holding amount
 * totalAmountBought: historical total amount purchased
 * totalAmountSold: historical total amount sold
 * stableTotalCost: total cost basis in USDT
 * currentPrice: current market price in USDT
 * currentPositionInUsdt: current market value in USDT
 * totalRealizedProfitUsdt: total realized profit/loss in USDT
 * unrealizedProfit: current unrealized profit/loss in USDT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CoinInformationResponse {

    private String coinName;
    private BigDecimal amount;
    private BigDecimal totalAmountBought;
    private BigDecimal totalAmountSold;
    private BigDecimal stableTotalCost;

    private BigDecimal currentPrice;
    private BigDecimal currentPositionInUsdt;

    private BigDecimal totalRealizedProfitUsdt;
    private BigDecimal unrealizedProfit;

    private int totalTransactions;
    private List<TransactionData> rows;

    public static CoinInformationResponse createEmpty(String coinName) {
        return CoinInformationResponse.builder()
                .amount(ZERO)
                .stableTotalCost(ZERO)
                .rows(new ArrayList<>())
                .coinName(coinName)
                .totalAmountBought(ZERO)
                .totalAmountSold(ZERO)
                .currentPositionInUsdt(ZERO)
                .totalRealizedProfitUsdt(ZERO)
                .unrealizedProfit(ZERO)
                .build();
    }

    public void addRows(TransactionData row) {
        rows.add(row);
        totalTransactions++;
    }

}
