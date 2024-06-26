package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.CryptoCompareResponse;
import com.importer.fileimporter.entity.PriceHistory;
import com.sun.istack.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class GetSymbolHistoricPriceService {

    private final String USDT = "USDT";
    private final String BTC = "BTC";

    private final CryptoCompareService cryptoCompareService;
    private final PriceHistoryService priceHistoryService;

    public BigDecimal getPriceInUsdt(String symbolPair, BigDecimal price, String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
        return getPriceInUsdt(symbolPair, price, dateTime);
    }

    public BigDecimal getPriceInUsdt(String symbolPair, BigDecimal price, LocalDateTime dateTime) {
        return getBigDecimal(symbolPair, price, dateTime, USDT);
    }

    public BigDecimal getPriceInBTC(String symbolPair, BigDecimal price, LocalDateTime dateTime) {
        return getBigDecimal(symbolPair, price, dateTime, BTC);
    }

    @NotNull
    private BigDecimal getBigDecimal(String symbolPair, BigDecimal price, LocalDateTime dateTime, String symbol) {
        try {
            BigDecimal priceInUsdt;
            Optional<PriceHistory> usdtPriceHistory = priceHistoryService.findData(symbolPair, symbol, dateTime);

            if (usdtPriceHistory.isEmpty()) {
                CryptoCompareResponse cryptoCompareResponse = cryptoCompareService.getHistoricalData(symbolPair, symbol,
                        dateTime.toEpochSecond(ZoneOffset.UTC));

                CryptoCompareResponse.ChartData exactTime = getExactTimeExecuted(dateTime, cryptoCompareResponse);
                priceInUsdt = exactTime.getHigh();
                priceHistoryService.saveAll(symbolPair, symbol, cryptoCompareResponse);
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
        return cryptoCompareResponse.getData().getChartDataList().stream()
                .filter(e -> e.getTime().getHour() == dateTime.getHour())
                .findFirst().orElse(cryptoCompareResponse.getData().getChartDataList().stream().findAny().get());
    }
}
