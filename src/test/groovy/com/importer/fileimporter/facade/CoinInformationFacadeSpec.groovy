package com.importer.fileimporter.facade

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.service.CoinInformationService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class CoinInformationFacadeSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def calculateAmountSpent = Mock(CalculateAmountSpent)
    def transactionService = Mock(TransactionService)
    def portfolioService = Mock(PortfolioService)

    def coinInformationService = new CoinInformationService(pricingFacade, holdingService, calculateAmountSpent, transactionService)

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
            realizedProfit == BigDecimal.ZERO
            unrealizedProfit == BigDecimal.ZERO
            currentPositionInUsdt == BigDecimal.ZERO
            avgEntryPrice.isEmpty()
        }
    }

    def "should calculate transaction information for a given symbol with buy and sell transactions"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transactions = getTestTransactions(portfolio)
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 2
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> {
            Transaction t, CoinInformationResponse response, Portfolio portfolio1
            -> t.paidAmount }

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

        1 * holdingService.save(_ as Holding) >> { Holding h ->
            assert h.amount == 80
            assert h.totalAmountBought == 200
            assert h.totalAmountSold == 120
            assert h.currentPositionInUsdt == 160
            h
        }
    }

    def "should calculate realized and unrealized profit correctly"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transactions = getTestTransactions(portfolio)
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 2
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> {
            Transaction t, CoinInformationResponse response, Portfolio portfolio1
                -> t.paidAmount }

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            totalAmountBought == 200
            totalAmountSold == 120
            currentPrice == 2
            currentPositionInUsdt == (currentPrice * amount)
            realizedProfit == -230.5
            unrealizedProfit == 160
            unrealizedTotalProfitMinusTotalCost == -40
        }
    }

    def "should handle a single buy transaction correctly"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transactions = [createTransaction(symbol, "USDT", "BUY", 1, 500, portfolio)]
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 500
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> 500

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            amount == 1
            realizedProfit == BigDecimal.ZERO
            unrealizedProfit == 500
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
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> 1000
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> {
            Transaction t, CoinInformationResponse response, Portfolio p -> t.paidAmount
        }

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            amount == BigDecimal.ZERO
            realizedProfit == -200
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