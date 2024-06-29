package com.importer.fileimporter.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@UtilityClass
public class OperationUtils {

    public static final List<String> SYMBOL = List.of("XVG", "BAND", "RSR", "AKRO", "DOT", "OP",
            "VET", "RLC", "BTC", "ETH");
    public static final List<String> GRAND_SYMBOLS = List.of("BTC", "ETH");
    public static final List<String> STABLE = List.of("USDT", "DAI", "BUSD", "UST", "USD", "USDC");

    public static final Predicate<String> IS_BUY = "BUY"::equals;

    public boolean isStable(String payedWithSymbol) {
        return STABLE.contains(payedWithSymbol);
    }

    public boolean isBuy(Map<?, ?> row) {
        String side = row.get("Side").toString();
        return IS_BUY.test(side);
    }

    public boolean isBuy(String side) {
        return IS_BUY.test(side);
    }

    public BigDecimal sumAmount(AtomicReference<BigDecimal> amountSpent, BigDecimal payedAmount, String side) {
        return sumAmount(amountSpent.get(), payedAmount, side);
    }

    public BigDecimal sumAmount(BigDecimal currentAmount, BigDecimal payedAmount, String side) {
        return isBuy(side) ? currentAmount.add(payedAmount) : currentAmount.subtract(payedAmount);
    }

    public BigDecimal accumulateExecutedAmount(BigDecimal currentAmount, BigDecimal executed, String side) {
        return isBuy(side) ? currentAmount.add(executed) : currentAmount.subtract(executed);
    }

    public static Optional<String> hasStable(String pair) {
        return STABLE.stream()
                .filter(pair::contains) // this is to catch the executed coin which should be at the beginning of the pair
                .findFirst();
    }
}
