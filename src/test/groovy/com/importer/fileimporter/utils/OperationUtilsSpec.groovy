package com.importer.fileimporter.utils

import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

import static com.importer.fileimporter.utils.OperationUtils.*

class OperationUtilsSpec extends Specification {

    def "test isStable with stable symbol"() {
        when:
        def result = isStable("USDT")

        then:
        result
    }

    def "test isStable with non-stable symbol"() {
        when:
        def result = isStable("BTC")

        then:
        !result
    }

    def "test isBuy with BUY side"() {
        given:
        def row = ["Side": "BUY"]

        when:
        def result = isBuy(row)

        then:
        result
    }

    def "test isBuy with SELL side"() {
        given:
        def row = ["Side": "SELL"]

        when:
        def result = isBuy(row)

        then:
        !result
    }

    def "test sumAmount when buying"() {
        given:
        def amountSpent = new AtomicReference<>(BigDecimal.TEN)

        when:
        def result = sumAmount(amountSpent, BigDecimal.ONE, "BUY")

        then:
        result == BigDecimal.valueOf(11)
    }

    def "test sumAmount when selling"() {
        given:
        def amountSpent = new AtomicReference<>(BigDecimal.TEN)

        when:
        def result = sumAmount(amountSpent, BigDecimal.ONE, "SELL")

        then:
        result == BigDecimal.valueOf(9)
    }

    def "test accumulateExecutedAmount when buying"() {
        when:
        def result = accumulateExecutedAmount(BigDecimal.TEN, BigDecimal.ONE, "BUY")

        then:
        result == BigDecimal.valueOf(11)
    }

    def "test accumulateExecutedAmount when selling"() {
        when:
        def result = accumulateExecutedAmount(BigDecimal.TEN, BigDecimal.ONE, "SELL")

        then:
        result == BigDecimal.valueOf(9)
    }

    def "test hasStable with matching pair"() {
        when:
        def result = hasStable("BTCUSDT")

        then:
        result.isPresent()
        result.get() == "USDT"
    }

    def "test hasStable with non-matching pair"() {
        when:
        def result = hasStable("ETHBTC")

        then:
        !result.isPresent()
    }
}
