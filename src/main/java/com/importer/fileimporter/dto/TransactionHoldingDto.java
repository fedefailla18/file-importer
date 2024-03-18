package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionHoldingDto {
    private String symbol;
    private BigDecimal amount;
    private BigDecimal buyPrice;
    private BigDecimal buyPriceInBtc;
    private BigDecimal sellPrice;
    private BigDecimal sellPriceInBtc;
    private BigDecimal payedInUsdt;
    private BigDecimal payedInBtc;
    private BigDecimal priceInBtc;
    private BigDecimal amountInBtc;
    private BigDecimal priceInUsdt;
    private BigDecimal amountInUsdt;
    private BigDecimal percentage;
}
