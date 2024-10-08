package com.importer.fileimporter.service

import com.importer.fileimporter.config.integration.CryptoCompareConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
class CryptoCompareServiceSpec extends Specification {

    @Autowired
    CryptoCompareConfig config

    @Autowired
    WebClient.Builder webClientBuilder

    @Autowired
    CryptoCompareProxy cryptoCompareService

    def "Test getHistoricalData method"() {
        given: "Input parameters for the historical data request"
        String fromSymbol = "BTC"
        String toSymbol = "USDT"
        long fromTimestamp = 1632589200L
        long toTimestamp = 1632625200L

        when: "Calling the getHistoricalData method"
        def response = cryptoCompareService.getHistoricalData(
                fromSymbol,
                toSymbol
                ,
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
        def response = cryptoCompareService.getData(
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
        def responses = cryptoCompareService.getData(
                fromSymbols,
                toSymbol
        )

        then: "The response should not be null"
        responses != null

        and: "The response should have 'Success' as the Response field"
        responses.every {
            it.key in ["BTC", "ETH", "ADA", "XRP"]
        }
    }
}
