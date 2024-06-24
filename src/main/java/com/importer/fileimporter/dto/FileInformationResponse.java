package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collection;

@Data
@Builder
public class FileInformationResponse {

    private int amount;

    private BigDecimal totalSpent;

    private Collection<CoinInformationResponse> coinInformationResponse;

}
