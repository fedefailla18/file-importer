package com.importer.fileimporter.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class ProcessFileUtils {

    public final String PAIR_KEY = "Pair";
    public final String EXECUTED_KEY = "Executed";
    public final String AMOUNT_KEY = "Amount";
    public final String FEE_KEY = "Fee";
    public final String PRICE_KEY = "Price";
    public final String DATE_KEY = "Date(UTC)";
    public final String SIDE_KEY = "Side";

    public BigDecimal getBigDecimalWithScale(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }

        BigDecimal bigDecimal;
        if (number instanceof BigDecimal) {
            bigDecimal = (BigDecimal) number;
        } else if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte) {
            bigDecimal = BigDecimal.valueOf(number.longValue());
        } else if (number instanceof Float || number instanceof Double) {
            bigDecimal = BigDecimal.valueOf(number.doubleValue());
        } else {
            throw new IllegalArgumentException("Unsupported number type: " + number.getClass().getName());
        }

        return bigDecimal.setScale(10, RoundingMode.UP);
    }

    public String getDate(Map<?, ?> row) {
        return row.get(DATE_KEY).toString();
    }

    public String getSide(Map<?, ?> row) {
        return row.get(SIDE_KEY).toString();
    }

    public static String getSymbolFromExecuted(Map<?, ?> row, List<String> symbols) {
        String executedString = row.get(EXECUTED_KEY).toString();

        // symbols makes it easy if you wanna import just a few symbols from the file
        if (symbols != null) {
            Optional<String> first = symbols.stream()
                    .filter(executedString::contains)
                    .findFirst();
            return first.orElseGet(() -> getSymbolFromNumber(executedString)); // TODO: here we should return null and handle
        }
        return getSymbolFromNumber(executedString);
    }

    public String getSymbolFromNumber(String feeString) {
        if (feeString == null || feeString.trim().isEmpty()) {
            throw new IllegalArgumentException("Fee string cannot be null or empty");
        }

        // Use a regular expression to match the numeric part
        String regex = "[0-9.,]+";
        String[] parts = feeString.split(regex);

        // If no parts found, raise an exception
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid fee string format");
        }

        // The symbol will be the non-numeric part at the end of the string
        String feeSymbol = parts[parts.length - 1];
        return feeSymbol;
    }

}