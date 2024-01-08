package com.importer.fileimporter.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@UtilityClass
public class OperationUtils {

    public static final List<String> SYMBOL = List.of("XVG", "BAND", "RSR", "AKRO", "DOT", "OP",
            "VET", "RLC", "BTC", "ETH");
    public static final List<String> GRAND_SYMBOLS = List.of("BTC", "ETH");
    public static final List<String> STABLE = List.of("USDT", "DAI", "BUSD", "UST", "USDC");

    public static final Predicate<String> IS_BUY = "BUY"::equals;

    public boolean isStable(String payedWithSymbol) {
        return OperationUtils.STABLE.contains(payedWithSymbol);
    }

    public boolean isBuy(Map<?, ?> row) {
        String side = row.get("Side").toString();
        return IS_BUY.test(side);
    }

    public boolean isBuy(String side) {
        return IS_BUY.test(side);
    }

    public static BigDecimal sumAmount(AtomicReference<BigDecimal> amountSpent, BigDecimal payedAmount, String side) {
        return sumAmount(amountSpent.get(), payedAmount, side);
    }

    public static BigDecimal sumAmount(BigDecimal amountSpent, BigDecimal payedAmount, String side) {
        return isBuy(side) ? amountSpent.add(payedAmount) : amountSpent.subtract(payedAmount);
    }
}
