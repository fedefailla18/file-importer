package com.importer.fileimporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioProcessingResult {

    private List<CoinInformationResponse> coinInformation;
    private int processedCount;
    private int totalTransactions;
}
