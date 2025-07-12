package com.importer.fileimporter.service

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.config.integration.CryptoCompareConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Subject

class CryptoCompareProxyIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    CryptoCompareConfig config

    @Autowired
    WebClient.Builder webClientBuilder

    @Subject
    CryptoCompareProxy sut

    def "Test getHistoricalData method"() {
        given: "Input parameters for the historical data request"
        String fromSymbol = "BTC"
        String toSymbol = "USDT"
        long fromTimestamp = 1632589200L
        long toTimestamp = 1632625200L

        when: "Calling the getHistoricalData method"
        def response = sut.getHistoricalData(
                fromSymbol,
                toSymbol,
                toTimestamp
        )

        then: "The response should not be null"
        response != null

        and: "The response should have 'Success' as the Response field"
        response.getResponse() == "Success"
    }

    def "Test getData method"() {
        given: "Input parameters for the historical data request"
        String fromSymbol = "BTC"
        def toSymbol = "USDT"

        when: "Calling the getHistoricalData method"
        def response = sut.getData(
                fromSymbol,
                toSymbol
        )

        then: "The response should not be null"
        response != null

        and: "The response should have 'Success' as the Response field"
        response.get(toSymbol) != null

        and:
        def fromSymbols = ["BTC", "ETH", "ADA", "XRP"]

        when: "Calling the getHistoricalData method"
        def responses = sut.getData(
                fromSymbols,
                toSymbol
        )

        then: "The response should not be null"
        responses != null

        and: "The response is present for all fromSymbols"
        fromSymbols.findAll {
            responses.get(it)
        }.size() == fromSymbols.size()

        and: "all fromSymbols response should have a USDT Double amount"
        fromSymbols.stream()
                .map {
                    responses.get(it)
                }.map {
            it["USDT"]
        }.filter {
            it != null && it instanceof Double
        }.collect().size() == fromSymbols.size()
    }
}
