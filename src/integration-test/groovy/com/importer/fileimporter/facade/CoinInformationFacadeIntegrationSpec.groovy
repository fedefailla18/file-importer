package com.importer.fileimporter.facade

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Subject

import java.time.LocalDateTime

class CoinInformationFacadeIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    @Subject
    CoinInformationFacade coinInformationFacade

    def setup() {
        def portfolio1 = Portfolio.builder()
                .name("Binance")
                .user(defaultUser)
                .creationDate(LocalDateTime.now())
                .build()
        def portfolio2 = Portfolio.builder()
                .name("Other")
                .user(defaultUser)
                .creationDate(LocalDateTime.now().minusMonths(6L))
                .build()
        portfolioRepository.save(portfolio1)
        portfolioRepository.save(portfolio2)
    }

    def "test getTransactionsInformation with predefined transactions"() {
        given: "a set of predefined transactions"
        def symbol = "BAND"
        def transactions = transactionService.getAllBySymbol(symbol)
        assert !transactions.isEmpty() : "No transactions found for symbol: ${symbol}"

        when: "getTransactionsInformation is called"
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then: "the response contains the correct information"
        response.coinName == symbol
        response.amount > 0
        response.stableTotalCost > 0
    }
}

