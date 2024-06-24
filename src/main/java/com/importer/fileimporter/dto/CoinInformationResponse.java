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
                .build();
    }

    public void addRows(Map<?, ?> row) {
        rows.add(row);
        totalTransactions++;
    }

    public void setAvgEntryPrice(String symbolPair, BigDecimal price, BigDecimal executed, boolean isBuy) {
        avgEntryPrice.computeIfAbsent(symbolPair, k -> BigDecimal.ZERO);

        avgEntryPrice.computeIfPresent(symbolPair, (k, v) -> {
            var pondering = price.multiply(executed);
            if (isBuy) {
                return pondering.add(v);
            } else {
                return pondering.subtract(v);
            }
        });
    }

    public void calculateAvgPrice() {
        BigDecimal reduce = avgEntryPrice.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(10, RoundingMode.UP);

        this.setTotalStable(reduce);

        if (!BigDecimal.ZERO.equals(this.getAmount())) {
            avgEntryPrice.put("AVG", reduce.divide(amount, 10, RoundingMode.HALF_UP));
        }
    }
}
