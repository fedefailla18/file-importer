package com.importer.fileimporter.utils

import spock.lang.Specification

import java.math.RoundingMode

import static ProcessFileUtils.*

class ProcessFileUtilsSpec extends Specification {

    def "test getBigDecimalWithScale"() {
        when:
        def result = getBigDecimalWithScale(123.456)

        then:
        result == BigDecimal.valueOf(123.456).setScale(10, RoundingMode.UP)
    }

    def "test getSymbolFromNumber with regular symbols"() {
        expect:
        getSymbolFromNumber(input) == expected

        where:
        input           | expected
        "21.6BAND"      | "BAND"
        "15.7BAND"      | "BAND"
        "11.67UNI"      | "UNI"
        "0.0001039BTC"  | "BTC"
        "0.000539BTC"   | "BTC"
        "0.01044562BTC" | "BTC"
    }

    def "test getSymbolFromNumber with special symbols containing numbers"() {
        expect:
        getSymbolFromNumber(input) == expected

        where:
        input           | expected
        "21.61INCH"     | "1INCH"
        "15.71INCH"     | "1INCH"
        "11.67API3"     | "API3"
        "0.0001039API3" | "API3"
    }

    def "test getSymbolFromNumber with null or empty input"() {
        when:
        getSymbolFromNumber(input)

        then:
        thrown(IllegalArgumentException)

        where:
        input << [null, "", "  "]
    }
}
