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
        def transactions = getTestTransactions()
        def portfolio = new Portfolio(name: "TestPortfolio")
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> new BigDecimal("2")
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> {
            Transaction t, CoinInformationResponse response, Portfolio portfolio1
            -> t.paidAmount }

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            coinName == "RLC"
            amount == new BigDecimal("80")
            totalAmountBought == new BigDecimal("200")
            totalAmountSold == new BigDecimal("120")
            currentPositionInUsdt == new BigDecimal("160")
        }

        1 * holdingService.save(_ as Holding) >> { Holding h ->
            assert h.amount == new BigDecimal("80")
            assert h.totalAmountBought == new BigDecimal("200")
            assert h.totalAmountSold == new BigDecimal("120")
            assert h.currentPositionInUsdt == new BigDecimal("160")
            h
        }
    }

    def "should calculate realized and unrealized profit correctly"() {
        given:
        def symbol = "RLC"
        def transactions = getTestTransactions()
        def portfolio = new Portfolio(name: "TestPortfolio")
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> new BigDecimal("2")
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> {
            Transaction t, CoinInformationResponse response, Portfolio portfolio1
                -> t.paidAmount }

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            realizedProfit == new BigDecimal("-230.5")
            unrealizedProfit == new BigDecimal("160")
            unrealizedTotalProfitMinusTotalCost == new BigDecimal("-40.5")
        }
    }

    def "should handle a single buy transaction correctly"() {
        given:
        def symbol = "RLC"
        def transactions = [createTransaction(symbol, "USDT", "BUY", new BigDecimal("1"), new BigDecimal("500"))]
        def portfolio = new Portfolio(name: "TestPortfolio")
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> new BigDecimal("500")
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> new BigDecimal("500")

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            amount == new BigDecimal("1")
            realizedProfit == BigDecimal.ZERO
            unrealizedProfit == new BigDecimal("500")
            currentPositionInUsdt == new BigDecimal("500")
        }
    }

    def "should handle multiple transactions on a single account"() {
        given:
        def symbol = "RLC"
        def transactions = [
                createTransaction(symbol, "USDT", "BUY", new BigDecimal("1"), new BigDecimal("500")),
                createTransaction(symbol, "USDT", "SELL", new BigDecimal("1"), new BigDecimal("200"))
        ]
        def portfolio = new Portfolio(name: "TestPortfolio")
        def holding = new Holding(symbol: symbol, portfolio: portfolio)

        transactionService.getAllBySymbol(symbol) >> transactions
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding
        pricingFacade.getCurrentMarketPrice(symbol) >> new BigDecimal("1000")
        calculateAmountSpent.getAmountSpentInUsdt(_, _, _) >> {
            Transaction t, CoinInformationResponse response, Portfolio portfolio1
                -> t.paidAmount }

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        with(response) {
            amount == BigDecimal.ZERO
            realizedProfit == new BigDecimal("-300")
            unrealizedProfit == BigDecimal.ZERO
            currentPositionInUsdt == BigDecimal.ZERO
        }
    }

    def static List<Transaction> getTestTransactions() {
        def portfolio = new Portfolio(name: "TestPortfolio")
        [
                new Transaction(side: "BUY", pair: "RLCUSDT", price: 1, executed: 200, symbol: "RLC", paidWith: "USDT", paidAmount: 200, feeAmount: 0.2, portfolio: portfolio),
                new Transaction(side: "SELL", pair: "RLCUSDT", price: 2.15, executed: 70, symbol: "RLC", paidWith: "USDT", paidAmount: 150.5, feeAmount: 0.15, portfolio: portfolio),
                new Transaction(side: "SELL", pair: "RLCUSDT", price: 1.60, executed: 50, symbol: "RLC", paidWith: "USDT", paidAmount: 80, feeAmount: 0.091371, portfolio: portfolio)
        ]
    }

    def static Transaction createTransaction(String symbol, String paidWith, String side, BigDecimal executed, BigDecimal price) {
        new Transaction(
                executed: executed,
                side: side,
                dateUtc: LocalDateTime.now(),
                price: price,
                symbol: symbol,
                paidWith: paidWith,
                paidAmount: executed * price,
                portfolio: new Portfolio(name: "TestPortfolio")
        )
    }
}