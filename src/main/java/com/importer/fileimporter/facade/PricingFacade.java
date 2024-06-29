package com.importer.fileimporter.facade;

import com.importer.fileimporter.entity.PriceHistory;
import com.importer.fileimporter.service.GetSymbolHistoricPriceHelper;
import com.importer.fileimporter.service.PriceHistoryService;
import com.importer.fileimporter.service.SymbolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.importer.fileimporter.service.GetSymbolHistoricPriceHelper.BTC;
import static com.importer.fileimporter.service.GetSymbolHistoricPriceHelper.USDT;

@Service
@RequiredArgsConstructor
public class PricingFacade {

    private final SymbolService symbolService;
    private final PriceHistoryService priceHistoryService;
    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceHelper;

    public BigDecimal getPrice(String symbol, String symbolPair, LocalDateTime dateTime) {
        if (StringUtils.trimAllWhitespace(symbol).isEmpty()) {
            symbol = BTC;
        }
        if (!StringUtils.hasText(symbolPair)) {
            symbolPair = USDT;
        }
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        String finalSymbol = symbol.toUpperCase();
        String finalSymbolPair = symbolPair.toUpperCase();
        LocalDateTime finalDateTime = dateTime;
        Optional<PriceHistory> data = priceHistoryService.findData(symbolPair, symbol, dateTime);
        return data
                .map(PriceHistory::getHigh)
                .orElseGet(() ->
                    getSymbolHistoricPriceHelper.getPricesAtDate(finalSymbol, finalSymbolPair,
                            finalDateTime.minusMinutes(1L)));
    }

    public Map<String, Double> getPrices(String symbol) {
        return getSymbolHistoricPriceHelper.getPrice(symbol);
    }
    public Map<String, Double> getPrices(List<String> symbol) {
        return getSymbolHistoricPriceHelper.getPrice(symbol);
    }

}
