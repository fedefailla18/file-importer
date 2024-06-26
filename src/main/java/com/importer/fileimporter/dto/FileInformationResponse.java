package com.importer.fileimporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInformationResponse {

    private int amount;

    private BigDecimal totalSpent;

    private Collection<CoinInformationResponse> coinInformationResponse;

}
