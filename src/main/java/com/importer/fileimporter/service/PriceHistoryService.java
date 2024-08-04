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
import java.util.List;
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
        String collect = repository.findAll().stream()
                .filter(e -> e.getSymbol().equals(symbolPair) &&
                        e.getSymbolpair().equals(usdt))
                .map(PriceHistory::getTime)
                .map(LocalDateTime::toString)
                .collect(Collectors.joining());
        return dataList.stream()
                .filter(e -> !collect.contains(e.getTime().toString()))
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

}
