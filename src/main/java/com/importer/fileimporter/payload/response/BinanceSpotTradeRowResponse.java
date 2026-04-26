package com.importer.fileimporter.payload.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BinanceSpotTradeRowResponse {
    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private String side;
    private Long tradeId;
    private Long orderId;
    private Long orderListId;
    private BigDecimal price;
    private BigDecimal qty;
    private BigDecimal quoteQty;
    private BigDecimal commission;
    private String commissionAsset;
    private Long time;
    private Boolean buyer;
    private Boolean maker;
    private Boolean bestMatch;
}
