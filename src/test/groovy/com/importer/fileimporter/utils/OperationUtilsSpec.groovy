package com.importer.fileimporter.utils

import com.importer.fileimporter.dto.BinanceTransactionAdapter
import com.importer.fileimporter.dto.MexcTransactionAdapter
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

    def "test isBuy BinanceTransactionAdapter"() {
        given:
        def row = ["Side": side, "Executed": "0.1BTC"]
        def adapter = new BinanceTransactionAdapter(row)

        when:
        def result = isBuy(adapter)

        then:
        result == expectedResult

        where:
        side   || expectedResult
        "BUY"  || true
        "SELL" || false
    }

    def "test isBuy MexcTransactionAdapter"() {
        given:
        def row = [
                "Pares": "BTC_USDT",
                "Dirección": side]
        def adapter = new MexcTransactionAdapter(row)

        when:
        def result = isBuy(adapter)

        then:
        result == expectedResult

        where:
        side     || expectedResult
        "COMPRA" || true
        "Compra" || true
        "VENTA"  || false
        "Venta"  || false
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
