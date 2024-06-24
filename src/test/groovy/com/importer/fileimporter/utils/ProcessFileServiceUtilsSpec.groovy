package com.importer.fileimporter.utils

import spock.lang.Specification

import java.math.RoundingMode

import static com.importer.fileimporter.utils.ProcessFileServiceUtils.*

class ProcessFileServiceUtilsSpec extends Specification {

    def "test getPair"() {
        given:
        def row = [Pair: 'BTCUSDT']

        when:
        def result = getPair(row)

        then:
        result == 'BTCUSDT'
    }

    def "test getExecuted"() {
        given:
        def row = [Executed: '170.0000000000FET']

        when:
        def result = getExecuted(row, 'FET')

        then:
        result == BigDecimal.valueOf(170.0000000000)
    }

    def "test getAmount"() {
        given:
        def row = [Amount: '225.59000000USDT']

        when:
        def result = getAmount(row, 'USDT')

        then:
        result == BigDecimal.valueOf(225.590000).setScale(10, RoundingMode.UP)
    }

    def "test getFee"() {
        given:
        def row = [Fee: '0.1657500000FET']

        when:
        def result = getFee(row)

        then:
        result == BigDecimal.valueOf(0.1657500000).setScale(10, RoundingMode.UP)
    }

    def "test getFeeSymbol"() {
        expect:
        getFeeSymbol(feeString, '') == expectedSymbol

        where:
        feeString               || expectedSymbol
        '0.1657500000FET'       || 'FET'
        '151.29000000USDT'      || 'USDT'
        '0.0000001200BTC'       || 'BTC'
        '1.23XYZ'               || 'XYZ'
        '0.00000123ETH'         || 'ETH'
        '12345.67asdf'          || 'asdf'
    }

    def "test getFeeSymbol should handle invalid input"() {
        when:
        getFeeSymbol(feeString, '')

        then:
        thrown(IllegalArgumentException)

        where:
        feeString << [null, '', '   ', '123.456', '10.00']
    }

    def "test getPrice"() {
        given:
        def row = [Price: '1.3270000000']

        when:
        def result = getPrice(row)

        then:
        result == BigDecimal.valueOf(1.3270000000).setScale(10, RoundingMode.UP)
    }

    def "test getDate"() {
        given:
        def row = ['Date(UTC)': '2024-06-17 22:32:10']

        when:
        def result = getDate(row)

        then:
        result == '2024-06-17 22:32:10'
    }

    def "test getSide"() {
        given:
        def row = [Side: 'BUY']

        when:
        def result = getSide(row)

        then:
        result == 'BUY'
    }

    def "test getSymbolFromExecuted"() {
        given:
        def row = [Executed: '170.0000000000FET']
        def symbols = ['FET', 'USDT', 'BTC']

        when:
        def result = getSymbolFromExecuted(row, symbols)

        then:
        result == 'FET'
    }

    def "test getBigDecimalWithScale"() {
        when:
        def result = getBigDecimalWithScale(123.456)

        then:
        result == BigDecimal.valueOf(123.456).setScale(10, RoundingMode.UP)
    }
}
