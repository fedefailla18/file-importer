package com.importer.fileimporter.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ProcessFileUtils {

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

    public String getSymbolFromNumber(String valueString) {
        if (valueString == null || valueString.trim().isEmpty()) {
            throw new IllegalArgumentException("Value string cannot be null or empty");
        }

        // List of known symbols that contain numbers
        List<String> specialSymbols = Arrays.asList("1INCH", "API3");

        // Check if the valueString contains any of the special symbols
        for (String specialSymbol : specialSymbols) {
            if (valueString.contains(specialSymbol)) {
                return specialSymbol;
            }
        }

        // Use a regular expression to match the numeric part
        String regex = "[0-9.,]+";
        String[] parts = valueString.split(regex);

        // If no parts found, raise an exception
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid value string format");
        }

        // The symbol will be the non-numeric part at the end of the string
        String symbol = parts[parts.length - 1];
        return symbol;
    }


}
