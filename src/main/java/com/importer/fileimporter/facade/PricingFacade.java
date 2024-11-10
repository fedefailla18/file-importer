package com.importer.fileimporter.facade;

import com.importer.fileimporter.entity.PriceHistory;
import com.importer.fileimporter.service.GetSymbolHistoricPriceHelper;
import com.importer.fileimporter.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.importer.fileimporter.utils.OperationUtils.BTC;
import static com.importer.fileimporter.utils.OperationUtils.USDT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingFacade {

    private final PriceHistoryService priceHistoryService;
    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceHelper;

    public BigDecimal getPriceInUsdt(String symbol, LocalDateTime dateTime) {
        return getPrice(symbol, USDT, dateTime);
    }
    public BigDecimal getPriceInBTC(String symbol, LocalDateTime dateTime) {
        return getPrice(symbol, BTC, dateTime);
    }

    public Optional<BigDecimal> findHighPrice(String symbol, String symbolPair, LocalDateTime dateTime) {
        return priceHistoryService.findHighPrice(symbol, symbolPair, dateTime);
    }

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
        return priceHistoryService.findData(symbolPair, symbol, dateTime)
                .map(PriceHistory::getHigh)
                .orElseGet(() ->
                    getSymbolHistoricPriceHelper.getPricesAtDate(finalSymbol, finalSymbolPair,
                            finalDateTime.minusMinutes(1L)));
    }

    public Map<String, Double> getPrices(String symbol) {
        try {
            Map<String, Number> price = getSymbolHistoricPriceHelper.getPrice(symbol);
            Map<String, Double> priceAsDouble = new HashMap<>();

            for (Map.Entry<String, Number> entry : price.entrySet()) {
                priceAsDouble.put(entry.getKey(), entry.getValue().doubleValue());
            }

            return priceAsDouble;
        } catch (java.lang.ClassCastException e) {
            log.error("Error in pricingFacade when getting prices. Symbol: " + symbol, e);
            return new HashMap<>();
        }
    }

    public BigDecimal getCurrentMarketPrice(String symbol) {
        return getSymbolHistoricPriceHelper.getCurrentMarketPriceInUSDT(symbol);
    }

    public Map<String, Double> getPrices(List<String> symbol) {
        return getSymbolHistoricPriceHelper.getPrice(symbol);
    }

}
