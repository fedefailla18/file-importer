package com.importer.fileimporter.dto.integration.binance;

import lombok.Data;
import java.util.List;

@Data
public class BinanceExchangeInfoResponse {
    private List<SymbolInfo> symbols;

    @Data
    public static class SymbolInfo {
        private String symbol;
        private String status;
        private String baseAsset;
        private String quoteAsset;
    }
}
