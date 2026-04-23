package com.importer.fileimporter.facade

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.service.CoinInformationService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.TransactionProcessor
import com.importer.fileimporter.facade.PricingFacade
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class CoinInformationFacadeSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def transactionProcessor = Mock(TransactionProcessor)
    def transactionService = Mock(TransactionService)
    def portfolioService = Mock(PortfolioService)

    def coinInformationService = new CoinInformationService(pricingFacade, holdingService, transactionProcessor)

    @Subject
    def coinInformationFacade = new CoinInformationFacade(transactionService, portfolioService, coinInformationService)

    def "should handle no transactions"() {
        given:
        def symbol = "RLC"
        transactionService.getAllBySymbol(symbol) >> []

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            coinName == symbol
            amount == BigDecimal.ZERO
            totalAmountBought == BigDecimal.ZERO
            totalAmountSold == BigDecimal.ZERO
            totalRealizedProfitUsdt == BigDecimal.ZERO
            unrealizedProfit == BigDecimal.ZERO
            currentPositionInUsdt == BigDecimal.ZERO
        }
    }

    def "should calculate transaction information for a given symbol with buy and sell transactions"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transactions = getTestTransactions(portfolio)
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getHolding(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 2
        holding.amount = 80
        holding.totalAmountBought = 200
        holding.totalAmountSold = 120
        holding.stableTotalCost = 100
        holding.totalRealizedProfitUsdt = 50

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            coinName == "RLC"
            amount == 80
            totalAmountBought == 200
            totalAmountSold == 120
            currentPositionInUsdt == 160
        }
    }

    def "should calculate realized and unrealized profit correctly"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transactions = getTestTransactions(portfolio)
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getHolding(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 2
        holding.amount = 80
        holding.totalAmountBought = 200
        holding.totalAmountSold = 120
        holding.stableTotalCost = 100
        holding.totalRealizedProfitUsdt = -230.5

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            totalAmountBought == 200
            totalAmountSold == 120
            currentPrice == 2
            currentPositionInUsdt == (currentPrice * amount)
            totalRealizedProfitUsdt == -230.5
            unrealizedProfit == 60
        }
    }

    def "should handle a single buy transaction correctly"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transactions = [createTransaction(symbol, "USDT", "BUY", 1, 500, portfolio)]
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getHolding(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 500
        holding.amount = 1
        holding.stableTotalCost = 500
        holding.totalRealizedProfitUsdt = 0
        holding.totalAmountBought = 1

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            amount == 1
            totalRealizedProfitUsdt == BigDecimal.ZERO
            unrealizedProfit == 0
            currentPositionInUsdt == 500
        }
    }

    def "should handle multiple transactions on a single account"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        def transactions = [
                createTransaction(symbol, "USDT", "BUY", 1, 500, portfolio),
                createTransaction(symbol, "USDT", "SELL", 1, 200, portfolio)
        ]

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getHolding(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 1000
        holding.amount = 0
        holding.stableTotalCost = 0
        holding.totalRealizedProfitUsdt = -300

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            amount == BigDecimal.ZERO
            totalRealizedProfitUsdt == -300
            unrealizedProfit == BigDecimal.ZERO
            currentPositionInUsdt == BigDecimal.ZERO
        }
    }

    def "should throw 404 when portfolio not found"() {
        given:
        portfolioService.getByName("missing") >> Optional.empty()

        when:
        coinInformationFacade.getPortfolioTransactionsInformation("missing")

        then:
        thrown(ResponseStatusException)
    }

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

        and: "CoinInformationService returns null for some symbols (processed transactions)"
        coinInformationService = Mock(CoinInformationService)
        coinInformationFacade = new CoinInformationFacade(transactionService, portfolioService, coinInformationService)

        coinInformationService.getCoinInformationResponse("BTC", _) >> null
        coinInformationService.getCoinInformationResponse("ETH", _) >> new CoinInformationResponse(coinName: "ETH")
        coinInformationService.getCoinInformationResponse("LTC", _) >> new CoinInformationResponse(coinName: "LTC")

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

        and: "CoinInformationService returns null for some symbols (processed transactions)"
        coinInformationService = Mock(CoinInformationService)
        coinInformationFacade = new CoinInformationFacade(transactionService, portfolioService, coinInformationService)

        coinInformationService.getCoinInformationResponse("BTC", _) >> null
        coinInformationService.getCoinInformationResponse("ETH", _) >> new CoinInformationResponse(coinName: "ETH")
        coinInformationService.getCoinInformationResponse("LTC", _) >> new CoinInformationResponse(coinName: "LTC")

        when: "Getting all transactions information"
        def result = coinInformationFacade.getTransactionsInformation()

        then: "The result should not contain null responses"
        result.size() == 2
        result.every { it != null }
        result.collect { it.coinName }.sort() == ["ETH", "LTC"]
    }

    def getTestTransactions(portfolio) {
        [
            new Transaction(side: "BUY",  pair: "RLCUSDT", price: 1,    executed: 200, symbol: "RLC", paidWith: "USDT", paidAmount: 200,   feeAmount: 0.2, portfolio: portfolio, processed: false),
            new Transaction(side: "SELL", pair: "RLCUSDT", price: 2.15, executed: 70,  symbol: "RLC", paidWith: "USDT", paidAmount: 150.5, feeAmount: 0.15, portfolio: portfolio, processed: false),
            new Transaction(side: "SELL", pair: "RLCUSDT", price: 1.60, executed: 50,  symbol: "RLC", paidWith: "USDT", paidAmount: 80,    feeAmount: 0.091371, portfolio: portfolio, processed: false)
        ]
    }

    def createTransaction(symbol, paidWith, side, executed, price, portfolio) {
        new Transaction(
                executed: executed,
                side: side,
                dateUtc: LocalDateTime.now(),
                price: price,
                symbol: symbol,
                paidWith: paidWith,
                paidAmount: executed * price,
                portfolio: portfolio,
                processed: false
        )
    }
}
