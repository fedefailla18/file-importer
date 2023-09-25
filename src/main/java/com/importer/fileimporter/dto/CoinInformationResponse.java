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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinInformationResponse {

    private String coinName;

    private BigDecimal amount;

    private Map<String, BigDecimal> avgEntryPrice = new HashMap<>();

    private BigDecimal usdSpent;

    private Map<String, BigDecimal> spent = new HashMap<>();

    private int totalTransactions;

    private List<Map<?, ?>> rows = new ArrayList<>();

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
        avgEntryPrice.forEach((k, v) ->
            this.avgEntryPrice.put(k,
                    v.divide(this.getAmount(), 5,
                            RoundingMode.HALF_UP))
        );
    }
}
