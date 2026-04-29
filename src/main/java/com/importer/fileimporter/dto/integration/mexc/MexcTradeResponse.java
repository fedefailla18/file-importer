package com.importer.fileimporter.dto.integration.mexc;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MexcTradeResponse {
    private String symbol;
    private Long id;
    private Long orderId;
    private BigDecimal price;
    private BigDecimal qty;
    private BigDecimal quoteQty;
    private BigDecimal commission;
    private String commissionAsset;
    private Long time;
    private Boolean isBuyer;
    private Boolean isMaker;
}
