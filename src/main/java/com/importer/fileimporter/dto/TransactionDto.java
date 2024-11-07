package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDto {
    private Long id;
    private LocalDateTime dateUtc;
    private String side;
    private String pair;
    private BigDecimal price;
    private BigDecimal executed;
    private String symbol;
    private String payedWith;
    private BigDecimal payedAmount;
    private String fee;
    private BigDecimal feeAmount;
    private String feeSymbol;
    private LocalDateTime created;
    private String createdBy;
    private LocalDateTime modified;
    private String modifiedBy;
    private boolean processed;
    private LocalDateTime lastProcessedAt;
}
