package com.importer.fileimporter.dto.integration.binance;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceConvertTradeResponse {
    private List<ConvertTrade> list;
    private Long startTime;
    private Long endTime;
    private Integer limit;
    private Boolean moreData;

    @Data
    public static class ConvertTrade {
        private String quoteId;
        private String orderId;
        private String orderStatus;
        private String fromAsset;
        private BigDecimal fromAmount;
        private String toAsset;
        private BigDecimal toAmount;
        private BigDecimal ratio;
        private BigDecimal inverseRatio;
        private Long createTime;
    }
}
