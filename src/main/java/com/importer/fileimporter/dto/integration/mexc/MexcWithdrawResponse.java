package com.importer.fileimporter.dto.integration.mexc;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MexcWithdrawResponse {
    private String id;
    private BigDecimal amount;
    private BigDecimal transactionFee;
    private String coin;
    private String network;
    private Integer status;
    private String address;
    private String txId;
    private String applyTime;
}
