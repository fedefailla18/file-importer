package com.importer.fileimporter.dto.integration.binance;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceAccountResponse {
    private List<AssetBalance> balances;

    @Data
    public static class AssetBalance {
        private String asset;
        private BigDecimal free;
        private BigDecimal locked;
    }
}
