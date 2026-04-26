package com.importer.fileimporter.dto.integration.binance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BinanceTradeResponse {
    private String symbol;
    private Long id;
    private Long orderId;
    private Long orderListId;
    private BigDecimal price;
    private BigDecimal qty;
    private BigDecimal quoteQty;
    private BigDecimal commission;
    private String commissionAsset;
    private Long time;
    private Boolean isBuyer;
    private Boolean isMaker;
    private Boolean isBestMatch;
}
