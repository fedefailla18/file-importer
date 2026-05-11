package com.importer.fileimporter.dto.integration.binance;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceFiatOrderResponse {
    private String code;
    private String message;
    private List<FiatOrder> data;
    private Integer total;
    private Boolean success;

    @Data
    public static class FiatOrder {
        private String orderNo;
        private String sourceAmount;
        private String fiatCurrency;
        private String obtainAmount;
        private String cryptoCurrency;
        private BigDecimal totalFee;
        private String price;
        private String status;
        private Long createTime;
        private Long updateTime;
    }
}
