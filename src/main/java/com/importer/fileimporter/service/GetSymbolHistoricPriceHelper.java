package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.CryptoCompareResponse;
import com.importer.fileimporter.entity.PriceHistory;
import com.sun.istack.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.importer.fileimporter.utils.OperationUtils.BTC;
import static com.importer.fileimporter.utils.OperationUtils.USDT;

@RequiredArgsConstructor
@Service
@Slf4j
public class GetSymbolHistoricPriceHelper {

    private final CryptoCompareProxy cryptoCompareProxy;
    private final PriceHistoryService priceHistoryService;

    public BigDecimal getPriceInUsdt(String symbol, BigDecimal price, LocalDateTime dateTime) {
        return getPriceBySymbol(symbol, price, dateTime, USDT);
    }

    public Map<String, Double> getPrice(String symbol) {
        String symbols;
        if (BTC.equals(symbol)) {
            symbols = USDT;
        } else if (USDT.equals(symbol)) {
            return Collections.emptyMap();
        } else {
            symbols = BTC + "," + USDT;
        }
        return getPricesAtDate(symbol, symbols);
    }

    @NotNull
    public BigDecimal getPricesAtDate(String fromSymbol, String toSymbol, LocalDateTime dateTime) {
        log.info(String.format("Fetching price for: %s, with: %s. Date: %s", toSymbol, fromSymbol, dateTime));
        CryptoCompareResponse cryptoCompareResponse = cryptoCompareProxy.getHistoricalData(fromSymbol, toSymbol,
                dateTime.toEpochSecond(ZoneOffset.UTC));

        CryptoCompareResponse.ChartData exactTime = getExactTimeExecuted(dateTime, cryptoCompareResponse);
        if (exactTime != null) {
            priceHistoryService.saveAll(fromSymbol, toSymbol, cryptoCompareResponse);
            return exactTime.getHigh().setScale(10, RoundingMode.DOWN);
        }
        return BigDecimal.ZERO;
    }

    @NotNull
    private Map<String, Double> getPricesAtDate(String symbol, String ...symbols) {
        String toSymbols = String.join(",", symbols);
        log.info("getting price for: " + symbol + " to: " + toSymbols);
        return cryptoCompareProxy.getData(symbol, toSymbols);
    }

    private BigDecimal getPriceBySymbol(String symbolPair, BigDecimal price, LocalDateTime dateTime, String symbol) {
        try {
            BigDecimal priceInUsdt;
            Optional<PriceHistory> usdtPriceHistory = priceHistoryService.findData(symbolPair, symbol, dateTime);

            if (usdtPriceHistory.isEmpty()) {
                priceInUsdt = getPricesAtDate(symbolPair, symbol, dateTime);
            } else {
                priceInUsdt = usdtPriceHistory.get().getHigh();
            }

        return price.multiply(priceInUsdt);
        } catch (Exception e ) {
            String msg = String.format("Error when requesting historical data for % on %", symbolPair, dateTime);
            log.error(msg, e);
            return BigDecimal.ZERO;
        }
    }

    private CryptoCompareResponse.ChartData getExactTimeExecuted(LocalDateTime dateTime, CryptoCompareResponse cryptoCompareResponse) {
        return cryptoCompareResponse.getResponse().equals("Error") ? null : cryptoCompareResponse.getData().getChartDataList().stream()
                .filter(e -> e.getTime().getHour() == dateTime.getHour())
                .findFirst()
                .orElseGet(() -> cryptoCompareResponse.getData()
                        .getChartDataList().stream()
                        .findAny()
                        .get());
    }

    public Map<String, Double> getPrice(List<String> symbol) {
        return cryptoCompareProxy.getData(symbol, BTC + "," + USDT);
    }
}
