package com.importer.fileimporter.facade

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.service.CoinInformationService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.TransactionService
import spock.lang.Specification
import spock.lang.Subject

class CoinInformationFacadeTest extends Specification {

    def transactionService = Mock(TransactionService)
    def portfolioService = Mock(PortfolioService)
    def coinInformationService = Mock(CoinInformationService)

    @Subject
    def coinInformationFacade = new CoinInformationFacade(transactionService, portfolioService, coinInformationService)

    def "getPortfolioTransactionsInformation should filter out null responses"() {
        given: "A portfolio with transactions for multiple symbols"
        def portfolioName = "TestPortfolio"
        def portfolio = new Portfolio(name: portfolioName)
        portfolioService.getByName(portfolioName) >> Optional.of(portfolio)

        and: "Transactions for multiple symbols"
        def transactions = [
            new Transaction(symbol: "BTC", portfolio: portfolio),
            new Transaction(symbol: "ETH", portfolio: portfolio),
            new Transaction(symbol: "LTC", portfolio: portfolio)
        ]
        transactionService.findByPortfolio(portfolio) >> transactions

        and: "The transactions are grouped by symbol"
        def btcTransactions = transactions.findAll { it.symbol == "BTC" }
        def ethTransactions = transactions.findAll { it.symbol == "ETH" }
        def ltcTransactions = transactions.findAll { it.symbol == "LTC" }

        and: "CoinInformationService returns null for some symbols (processed transactions)"
        coinInformationService.getCoinInformationResponse("BTC", btcTransactions) >> null
        coinInformationService.getCoinInformationResponse("ETH", ethTransactions) >> new CoinInformationResponse(coinName: "ETH")
        coinInformationService.getCoinInformationResponse("LTC", ltcTransactions) >> new CoinInformationResponse(coinName: "LTC")

        when: "Getting portfolio transactions information"
        def result = coinInformationFacade.getPortfolioTransactionsInformation(portfolioName)

        then: "The result should not contain null responses"
        result.size() == 2
        result.every { it != null }
        result.collect { it.coinName }.sort() == ["ETH", "LTC"]
    }

    def "getTransactionsInformation should filter out null responses"() {
        given: "Transactions for multiple symbols"
        def transactions = [
            new Transaction(symbol: "BTC"),
            new Transaction(symbol: "ETH"),
            new Transaction(symbol: "LTC")
        ]
        transactionService.getAll() >> transactions

        and: "The transactions are grouped by symbol"
        def btcTransactions = transactions.findAll { it.symbol == "BTC" }
        def ethTransactions = transactions.findAll { it.symbol == "ETH" }
        def ltcTransactions = transactions.findAll { it.symbol == "LTC" }

        and: "CoinInformationService returns null for some symbols (processed transactions)"
        coinInformationService.getCoinInformationResponse("BTC", btcTransactions) >> null
        coinInformationService.getCoinInformationResponse("ETH", ethTransactions) >> new CoinInformationResponse(coinName: "ETH")
        coinInformationService.getCoinInformationResponse("LTC", ltcTransactions) >> new CoinInformationResponse(coinName: "LTC")

        when: "Getting all transactions information"
        def result = coinInformationFacade.getTransactionsInformation()

        then: "The result should not contain null responses"
        result.size() == 2
        result.every { it != null }
        result.collect { it.coinName }.sort() == ["ETH", "LTC"]
    }
}