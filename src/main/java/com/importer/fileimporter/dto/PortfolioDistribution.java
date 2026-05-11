package com.importer.fileimporter.dto;

import com.importer.fileimporter.entity.ExchangeName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
@Builder
public class PortfolioDistribution {
    private String portfolioName;
    private ExchangeName exchangeName;
    private BigDecimal totalUsdt;
    private BigDecimal totalBuySpentUsdt;
    private BigDecimal totalSellEarnedUsdt;
    private LocalDateTime oldestTransactionDate;
    private LocalDateTime newestTransactionDate;

    public int getTotalHoldings() {
        return holdings.size();
    }

    public BigDecimal getTotalInUsdt() {
        return holdings.stream()
                .map(HoldingDto::getAmountInUsdt)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalInBTC() {
        return holdings.stream()
                .map(HoldingDto::getAmountInBtc)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    List<HoldingDto> holdings;

    public BigDecimal getNetCapitalFromPocket() {
        BigDecimal buys = totalBuySpentUsdt != null ? totalBuySpentUsdt : BigDecimal.ZERO;
        BigDecimal sells = totalSellEarnedUsdt != null ? totalSellEarnedUsdt : BigDecimal.ZERO;
        return buys.subtract(sells);
    }

    public BigDecimal getTotalRealizedProfitUsdt() {
        if (holdings == null) return BigDecimal.ZERO;
        return holdings.stream()
                .map(HoldingDto::getTotalRealizedProfitUsdt)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalUnrealizedProfitUsdt() {
        if (holdings == null) return BigDecimal.ZERO;
        return holdings.stream()
                .map(HoldingDto::getUnrealizedProfitUsdt)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void calculateHoldingPercent() {
        BigDecimal totalUsdt = this.getTotalInUsdt();
        if (totalUsdt == null) {
            return;
        }
        this.setTotalUsdt(totalUsdt);
        this.getHoldings().forEach(
                e -> {
                    if (e.getAmountInUsdt() == null || BigDecimal.ZERO.equals(totalUsdt)) {
                        return;
                    }
                    e.setPercentage(e.getAmountInUsdt()
                            .divide(totalUsdt, 7, RoundingMode.DOWN)
                            .multiply(new BigDecimal(100)));
                }
        );
    }

}
