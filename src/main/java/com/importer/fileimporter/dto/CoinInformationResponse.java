package com.importer.fileimporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ZERO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinInformationResponse {

    private String coinName;

    private BigDecimal amount;

    private Map<String, BigDecimal> avgEntryPrice;

    private BigDecimal usdSpent;

    private BigDecimal totalStable;

    private Map<String, BigDecimal> spent;

    private BigDecimal totalExecuted; // Track total executed amount for average calculation

    private int totalTransactions;

    private List<Map<?, ?>> rows;

    public static CoinInformationResponse createEmpty(String coinName) {
        return CoinInformationResponse.builder()
                .amount(ZERO)
                .usdSpent(ZERO)
                .rows(new ArrayList<>())
                .coinName(coinName)
                .spent(new HashMap<>())
                .avgEntryPrice(new HashMap<>())
                .totalExecuted(ZERO)
                .build();
    }

    public void addRows(Map<?, ?> row) {
        rows.add(row);
        totalTransactions++;
    }

    public void setAvgEntryPrice(String payedWith, BigDecimal priceInUsdt, BigDecimal executed, boolean isBuy) {
        if (isBuy) {
            usdSpent = usdSpent.add(priceInUsdt.multiply(executed));
            amount = amount.add(executed);
        } else {
            usdSpent = usdSpent.subtract(priceInUsdt.multiply(executed));
            amount = amount.subtract(executed);
        }
    }

    public void calculateAvgPrice() {
        if (totalExecuted.compareTo(BigDecimal.ZERO) > 0) {
            totalStable = usdSpent.divide(totalExecuted, 10, RoundingMode.HALF_UP);
        }
    }
}
