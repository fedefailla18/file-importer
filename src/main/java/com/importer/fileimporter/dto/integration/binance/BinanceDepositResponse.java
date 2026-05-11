package com.importer.fileimporter.dto.integration.binance;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BinanceDepositResponse {
    private BigDecimal amount;
    private String coin;
    private String network;
    private Integer status;
    private String address;
    private String txId;
    private Long insertTime;
}
