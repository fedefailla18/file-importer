package com.importer.fileimporter.payload.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BinanceSpotActivityResponse {
    private BinanceSpotActivitySummaryResponse summary;
    private List<BinanceAssetBalanceResponse> balances;
    private List<BinanceSpotTradeRowResponse> trades;
}
