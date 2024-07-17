package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Data
@Builder
public class PortfolioDistribution {
    private String portfolioName;
    private BigDecimal totalUsdt;

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
