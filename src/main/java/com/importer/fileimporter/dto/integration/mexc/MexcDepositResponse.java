package com.importer.fileimporter.dto.integration.mexc;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MexcDepositResponse {
    private BigDecimal amount;
    private String coin;
    private String network;
    private Integer status;
    private String address;
    private String txId;
    private Long insertTime;
}
