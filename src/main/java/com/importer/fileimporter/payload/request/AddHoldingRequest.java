package com.importer.fileimporter.payload.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddHoldingRequest {
    private String symbol;
    private String name;
    private BigDecimal amount;
    private String portfolio;
}
