package com.importer.fileimporter.dto

import spock.lang.Specification
import spock.lang.Unroll

class BinanceTransactionAdapterSpec extends Specification {

    def "should extract date correctly"() {
        given:
        def row = ["Date(UTC)": "2024-06-17 22:32:10", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getDate()

        then:
        result == "2024-06-17 22:32:10"
    }

    def "should extract pair correctly"() {
        given:
        def row = ["Pair": "FETUSDT", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getPair()

        then:
        result == "FETUSDT"
    }

    def "should extract side correctly"() {
        given:
        def row = ["Side": "BUY", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getSide()

        then:
        result == "BUY"
    }

    def "should extract price correctly"() {
        given:
        def row = ["Price": "1.3270000000", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getPrice()

        then:
        result == new BigDecimal("1.3270000000").setScale(10, BigDecimal.ROUND_UP)
    }

    def "should extract price correctly with comma"() {
        given:
        def row = ["Price": "1,327.0000000000", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getPrice()

        then:
        result == new BigDecimal("1327.0000000000").setScale(10, BigDecimal.ROUND_UP)
    }

    def "should extract executed amount correctly"() {
        given:
        def row = [
            "Executed": "170.0000000000FET",
            "Pair": "FETUSDT"
        ]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getExecuted()

        then:
        result == new BigDecimal("170.0000000000")
    }

    def "should extract amount correctly"() {
        given:
        def row = [
            "Amount": "225.59000000USDT",
            "Executed": "170.0000000000FET"
        ]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getAmount()

        then:
        result == new BigDecimal("225.59000000").setScale(10, BigDecimal.ROUND_UP)
    }

    def "should extract fee correctly"() {
        given:
        def row = ["Fee": "0.1657500000FET", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getFee()

        then:
        result == new BigDecimal("0.1657500000").setScale(10, BigDecimal.ROUND_UP)
    }

    def "should extract symbol correctly"() {
        given:
        def row = ["Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getSymbol()

        then:
        result == "FET"
    }

    def "should extract fee symbol correctly"() {
        given:
        def row = ["Fee": "0.1657500000FET", "Executed": "170.0000000000FET"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getFeeSymbol()

        then:
        result == "FET"
    }

    def "should extract paid with correctly"() {
        given:
        def row = [
            "Pair": "FETUSDT",
            "Executed": "170.0000000000FET"
        ]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getPaidWith()

        then:
        result == "USDT"
    }

    @Unroll
    def "should extract symbol correctly for special cases: #executedValue"() {
        given:
        def row = ["Executed": executedValue]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = adapter.getSymbol()

        then:
        result == expectedSymbol

        where:
        executedValue         | expectedSymbol
        "170.0000000000FET"   | "FET"
        "33.81INCH"           | "1INCH"
        "\"33.81INCH\""       | "1INCH"
        "100.0000000000API3"  | "API3"
    }
}