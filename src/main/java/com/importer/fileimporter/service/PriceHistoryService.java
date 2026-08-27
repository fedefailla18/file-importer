package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.CryptoCompareResponse;
import com.importer.fileimporter.entity.PriceHistory;
import com.importer.fileimporter.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class PriceHistoryService {

    private final PriceHistoryRepository repository;

    @Async("asyncExecutor")
    public void saveAll(String symbolPair, String usdt, CryptoCompareResponse cryptoCompareResponse) {

        List<CryptoCompareResponse.ChartData> dataList = cryptoCompareResponse.getData().getChartDataList();

        List<CryptoCompareResponse.ChartData> chartDataNotStored = validateData(symbolPair, usdt, dataList);

        if (chartDataNotStored.isEmpty()) {
            log.info("No new data to store");
            return;
        }

        List<PriceHistory> pricesHistory = chartDataNotStored.stream().map(e -> {
            return PriceHistory.builder()
                    .time(e.getTime())
                    .pair(symbolPair + usdt)
                    .symbol(symbolPair)
                    .symbolpair(usdt)
                    .high(e.getHigh())
                    .low(e.getLow())
                    .open(e.getOpen())
                    .close(e.getClose())
                    .volumeto(e.getVolumeto())
                    .volumefrom(e.getVolumefrom())
                    .created(LocalDateTime.now())
                    .modified(LocalDateTime.now())
                    .createdBy("Request to CryptoCompare when Processing File")
                    .modifiedBy("Request to CryptoCompare when Processing File")
                    .build();
        }).collect(Collectors.toList());

        repository.saveAll(pricesHistory);
    }

    private List<CryptoCompareResponse.ChartData> validateData(String symbolPair, String usdt, List<CryptoCompareResponse.ChartData> dataList) {
        return dataList.stream()
                .filter(e -> !repository.existsBySymbolAndSymbolpairAndTime(symbolPair, usdt, e.getTime()))
                .collect(Collectors.toList());
    }

    public Optional<PriceHistory> findData(String symbolPair, String pair, LocalDateTime dateTime) {
        LocalDateTime localDateTime = dateTime.withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        List<PriceHistory> allBySymbolAndSymbolPair = repository.findAllBySymbolAndSymbolpairAndTime(pair, symbolPair, localDateTime);
        return allBySymbolAndSymbolPair.stream()
                .findFirst();
    }

    public Optional<BigDecimal> findHighPrice(String symbolPair, String pair, LocalDateTime dateTime) {
        LocalDateTime localDateTime = dateTime.withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        return Optional.ofNullable(repository.findHighestPriceBySymbolAndSymbolpairAndTime(pair, symbolPair, localDateTime));
    }

    public Map<String, BigDecimal> findAllForWarmup(String symbol, String symbolPair, List<LocalDateTime> hours) {
        if (hours == null || hours.isEmpty()) return Collections.emptyMap();
        return repository.findBySymbolAndPairAndHoursIn(symbol.toUpperCase(), symbolPair.toUpperCase(), hours)
                .stream()
                .collect(Collectors.toMap(
                    ph -> ph.getSymbol() + ":" + ph.getSymbolpair() + ":"
                          + ph.getTime().truncatedTo(ChronoUnit.HOURS),
                    PriceHistory::getHigh,
                    (a, b) -> a
                ));
    }

}
