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
}
