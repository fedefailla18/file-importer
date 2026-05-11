package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.PriceHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.importer.fileimporter.config.CacheConfig.HISTORICAL_PRICE_CACHE;

/**
 * Redis-backed cache for historical crypto prices.
 * Historical prices are immutable, so entries are stored with no TTL (persist forever).
 * Spring AOP intercepts @Cacheable methods — no cache logic inside method bodies.
 */
@Service
@RequiredArgsConstructor
public class HistoricalPriceCacheService {

    private final PriceHistoryService priceHistoryService;
    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceHelper;

    @Cacheable(
        value = HISTORICAL_PRICE_CACHE,
        key = "'hp:' + #symbol.toUpperCase() + ':' + #symbolPair.toUpperCase() + ':' + #dateTime.truncatedTo(T(java.time.temporal.ChronoUnit).HOURS)",
        unless = "#result == null || #result.compareTo(T(java.math.BigDecimal).ZERO) == 0"
    )
    public BigDecimal lookup(String symbol, String symbolPair, LocalDateTime dateTime) {
        return priceHistoryService.findData(symbolPair, symbol, dateTime)
                .map(PriceHistory::getHigh)
                .orElseGet(() ->
                    getSymbolHistoricPriceHelper.getPricesAtDate(
                        symbol.toUpperCase(), symbolPair.toUpperCase(), dateTime.minusMinutes(1L)));
    }

    @CachePut(
        value = HISTORICAL_PRICE_CACHE,
        key = "'hp:' + #symbol.toUpperCase() + ':' + #symbolPair.toUpperCase() + ':' + #dateTime.truncatedTo(T(java.time.temporal.ChronoUnit).HOURS)"
    )
    public BigDecimal put(String symbol, String symbolPair, LocalDateTime dateTime, BigDecimal price) {
        return price;
    }

    static String cacheKey(String symbol, String symbolPair, LocalDateTime dateTime) {
        return "hp:" + symbol.toUpperCase() + ":" + symbolPair.toUpperCase() + ":"
                + dateTime.truncatedTo(ChronoUnit.HOURS);
    }
}
