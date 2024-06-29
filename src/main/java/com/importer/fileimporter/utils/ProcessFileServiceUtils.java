package com.importer.fileimporter.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class ProcessFileServiceUtils {

    public final String PAIR_KEY = "Pair";
    public final String EXECUTED_KEY = "Executed";
    public final String AMOUNT_KEY = "Amount";
    public final String FEE_KEY = "Fee";
    public final String PRICE_KEY = "Price";
    public final String DATE_KEY = "Date(UTC)";
    public final String SIDE_KEY = "Side";

    public String getPair(Map<?, ?> row) {
        return row.get(PAIR_KEY).toString();
    }

    public BigDecimal getExecuted(Map<?, ?> row, String coinName) {
        String executed = row.get(EXECUTED_KEY).toString()
                .replace(coinName, "")
                .replace(",", "");
        double added = Double.parseDouble(executed);
        return BigDecimal.valueOf(added);
    }

    public BigDecimal getAmount(Map<?, ?> row, String symbolPair) {
        String amount1 = row.get(AMOUNT_KEY).toString();
        String amount = amount1
                .substring(0, amount1.length() - 6)
                .replace(",", "");
        return getBigDecimalWithScale(Double.valueOf(amount));
    }

    public BigDecimal getFee(Map<?, ?> row) {
        String feeString = row.get(FEE_KEY).toString();
        String fee = feeString.substring(0, feeString.length() - 6);
        return getBigDecimalWithScale(Double.valueOf(fee));
    }

    public String getFeeSymbol(String feeString, String symbol) {
        // Check if the feeString is empty or null
        String feeSymbol = getSymbolFromNumber(feeString);

        // Check if the symbol is non-numeric and non-empty
        if (feeSymbol.matches("\\s*")) {
            throw new IllegalArgumentException("No symbol found in fee string");
        }

        return feeSymbol.trim();
    }

    public BigDecimal getPrice(Map<?, ?> row) {
        String price = row.get(PRICE_KEY).toString()
                .replace(",", "");
        return getBigDecimalWithScale(Double.valueOf(price));
    }

    public BigDecimal getBigDecimalWithScale(Number number) {
        // Validate input: null check
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }

        // Convert Number to BigDecimal
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

        // Set scale to 10 and use RoundingMode.UP
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

    private String getSymbolFromNumber(String feeString) {
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