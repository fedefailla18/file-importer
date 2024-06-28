package com.importer.fileimporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.importer.fileimporter.utils.serializer.Satoshi;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionHoldingDto {
    private String symbol;
    private BigDecimal amount;
    private BigDecimal buyPrice;

    @JsonProperty("buyPriceInBtc")
    @JsonSerialize(using = Satoshi.class)
    private BigDecimal buyPriceInBtc;

    private BigDecimal sellPrice;

    @JsonProperty("sellPriceInBtc")
    @JsonSerialize(using = Satoshi.class)
    private BigDecimal sellPriceInBtc;

    private BigDecimal payedInUsdt;
    private BigDecimal payedInBtc;
    private BigDecimal priceInBtc;
    private BigDecimal amountInBtc;
    private BigDecimal priceInUsdt;
    private BigDecimal amountInUsdt;
    private BigDecimal percentage;

    public static TransactionHoldingDto emptyTransactionHoldingDto(String symbol, BigDecimal totalAmount) {
        return TransactionHoldingDto.builder()
                .symbol(symbol)
                .amount(totalAmount)
                .buyPrice(BigDecimal.ZERO)
                .buyPriceInBtc(BigDecimal.ZERO)
                .sellPrice(BigDecimal.ZERO)
                .sellPriceInBtc(BigDecimal.ZERO)
                .payedInUsdt(BigDecimal.ZERO)
                .payedInBtc(BigDecimal.ZERO)
                .build();
    }
}
