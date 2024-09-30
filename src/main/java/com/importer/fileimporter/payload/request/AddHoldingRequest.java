package com.importer.fileimporter.payload.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AddHoldingRequest {
    @NotBlank(message = "Symbol is mandatory")
    private String symbol;

    private String name;

    @NotNull(message = "Amount is mandatory")
    private BigDecimal amount;

    @NotBlank(message = "Portfolio is mandatory")
    private String portfolio;

    private BigDecimal costInUsdt;
    private BigDecimal costInBtc;
}
