package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HoldingDto {
    private String symbol;
    private String portfolioName;
    private BigDecimal amount;
    private BigDecimal amountInBtc;
    private BigDecimal amountInUsdt;
    private BigDecimal priceInBtc;
    private BigDecimal priceInUsdt;
    private BigDecimal percentage;
}
