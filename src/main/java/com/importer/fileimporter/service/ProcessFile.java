package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.math.BigDecimal.ZERO;

@AllArgsConstructor
@Service
@Slf4j
public class ProcessFile {

//    public static final List<String> SYMBOL = List.of("XVG");
    public static final List<String> SYMBOL = List.of("XVG", "BAND", "RSR", "AKRO", "DOT", "OP", "WAVES",
            "VET", "RLC", "BTC", "ETH");
    public static final List<String> GRAND_SYMBOLS = List.of("BTC", "ETH");
    public static final List<String> STABLE = List.of("USDT", "DAI", "BUSD", "UST", "USDC");

    public static final Predicate<String> IS_BUY = "BUY"::equals;
    public static final Predicate<String> IS_SELL = "SELL"::equals;

    private final FileImporterService fileImporterService;

    public FileInformationResponse processFile(MultipartFile file) throws IOException {
        List<Map<?, ?>> rows = fileImporterService.getRows(file);
//        Map<String, AtomicInteger> counter = getAmountOfTransactionsByCoin(rows);
        Map<String, CoinInformationResponse> transactionDetailInformation = getTransactionDetailInformation(rows);

        calculateAvgPrice(transactionDetailInformation);

        return FileInformationResponse.builder()
//                .rows(rows)
//                .each(counter)
                .amount(rows.size())
                .coinInformationResponse(transactionDetailInformation.values())
                .build();
    }

    private void calculateAvgPrice(Map<String, CoinInformationResponse> transactionDetailInformation) {
        transactionDetailInformation.values().stream().parallel()
                .forEach(detail -> {
                    final var symbol = detail.getCoinName();
                    detail.getRows().stream()
                            .forEach(row -> {
                                String pair = getPair(row);
                                String symbolPair = pair.replace(symbol, "");
                                if (isStable(symbolPair).isPresent()) {
                                    String side = getSide(row);
                                    var executed = getExecuted(row, symbol);
                                    BigDecimal price = getPrice(row);

                                    detail.setAvgEntryPrice(symbolPair, price, executed,
                                            IS_BUY.test(side));
                                } else {
                                    log.info("No a Stable Coin transaction. " + symbolPair);
                                }
                            });
                    detail.calculateAvgPrice();
                });
    }

    private Map<String, CoinInformationResponse> getTransactionDetailInformation(List<Map<?, ?>> rows) {
        Map<String, CoinInformationResponse> informationResponseMap = new HashMap<>();

        rows.forEach(row -> {
            String pair = getPair(row);
            final var symbol = findTokenTransaction(pair);
            symbol.ifPresent(s -> {
                String symbolPair = pair.replace(s, "");
                String side = getSide(row);

                if (!informationResponseMap.containsKey(s)) {
                    informationResponseMap.computeIfAbsent(s, k -> {
                        var a = CoinInformationResponse.builder()
                                .amount(ZERO)
                                .usdSpent(ZERO)
                                .rows(new ArrayList<>())
                                .coinName(k)
                                .spent(new HashMap<>())
                                .avgEntryPrice(new HashMap<>())
                                .build();
                        a.setAmount(calculateAmount(a.getAmount(), side, getExecuted(row, s)));

                        Optional<String> first = STABLE.stream().filter(symbolPair::contains).findFirst();
                        first.ifPresent(value -> a.setUsdSpent(getAmountSpent(a.getUsdSpent(), getAmount(row, value), side)));

                        calculateSpent(getAmount(row, symbolPair), a, symbolPair, side);

                        a.addRows(row);
                        return a;
                    });
                    return;
                } //else {
                    CoinInformationResponse a = informationResponseMap.get(s);
                    a.setAmount(calculateAmount(a.getAmount(), side, getExecuted(row, s)));

                    Optional<String> first = STABLE.stream().filter(symbolPair::contains).findFirst();
                    if (first.isPresent()) {
                        a.setUsdSpent(getAmountSpent(a.getUsdSpent(), getAmount(row, first.get()), side));
                    }

                    calculateSpent(getAmount(row, symbolPair), a, symbolPair, side);
                    a.addRows(row);
                //}
            });
        });
        return informationResponseMap;
    }

    private Optional<String> findTokenTransaction(String pair) {
        return this.SYMBOL.stream()
                .filter(pair.substring(0,4)::contains) // this is to catch the executed coin which should be at the beginning of the pair
                .findFirst();
    }

    private Optional<String> isStable(String pair) {
        return this.STABLE.stream()
                .filter(pair::contains) // this is to catch the executed coin which should be at the beginning of the pair
                .findFirst();
    }

    private String getPair(Map<?, ?> row) {
        return row.get("Pair").toString();
    }

    private void calculateSpent(BigDecimal amount, CoinInformationResponse a, String symbolPair, String side) {
        a.getSpent().computeIfAbsent(symbolPair, k -> new BigDecimal(0));
        if (IS_BUY.test(side)) {
            a.getSpent().computeIfPresent(symbolPair, (k, v) -> v.add(amount));
        } else if (IS_SELL.test(side)){
            a.getSpent().computeIfPresent(symbolPair, (k, v) -> v.subtract(amount));
        }
    }

    private BigDecimal getAmountSpent(BigDecimal usdSpent, BigDecimal amount, String side) {
        if (IS_BUY.test(side)) {
            return usdSpent.add(amount);
        } else if (IS_SELL.test(side)){
            return usdSpent.subtract(amount);
        }
        return null;
    }

    private BigDecimal calculateAmount(BigDecimal aAmount, String side, BigDecimal amountAdded) {
        if (IS_BUY.test(side)) {
            return aAmount.add(amountAdded);
        } else if (IS_SELL.test(side)) {
            return aAmount.subtract(amountAdded);
        }
        return ZERO;
    }

    private Map<String, AtomicInteger> getAmountOfTransactionsByCoin(List<Map<?, ?>> rows) {
        Map<String, AtomicInteger> counter = new HashMap<>();
        countCoinPairTransactions(SYMBOL.get(0), rows, counter);
        countCoinPairTransactions(SYMBOL.get(1), rows, counter);
        countCoinPairTransactions(SYMBOL.get(2), rows, counter);
        return counter;
    }

    private void countCoinPairTransactions(String symbol, List<Map<?, ?>> rows, Map<String, AtomicInteger> counter) {
        rows.forEach(e -> {
            String pair = e.get("Pair").toString();
            boolean contains = pair.contains(symbol);
            if (contains) {
                counter.computeIfAbsent(symbol, k -> new AtomicInteger(0)).incrementAndGet();
            }
        });
    }

    private String getSide(Map<?, ?> row) {
        return row.get("Side").toString();
    }

    private BigDecimal getExecuted(Map<?, ?> row, String coinName) {
        String executed = row.get("Executed").toString();
        float added = Float.parseFloat(executed.replace(coinName, "").replace(",", ""));
        return BigDecimal.valueOf(added);
    }

    private BigDecimal getAmount(Map<?, ?> row, String symbolPair) {
        String executed = row.get("Amount").toString();
        float added = Float.parseFloat(executed.replace(symbolPair, "").replace(",", ""));
        return BigDecimal.valueOf(added);
    }

    private BigDecimal getPrice(Map<?, ?> row) {
        String price = row.get("Price").toString();
        return BigDecimal.valueOf(Float.parseFloat(price.replace(",", "")));
    }
}
