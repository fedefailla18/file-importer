package com.importer.fileimporter.payload.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MexcSpotActivityResponse {
    private MexcSpotActivitySummaryResponse summary;
    private List<MexcAssetBalanceResponse> balances;
    private List<MexcSpotTradeRowResponse> trades;
}
