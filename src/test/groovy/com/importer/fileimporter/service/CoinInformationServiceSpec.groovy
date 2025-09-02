package com.importer.fileimporter.service

import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import spock.lang.Specification

class CoinInformationServiceSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def calculateAmountSpent = new CalculateAmountSpent(pricingFacade, holdingService)
    def transactionService = Mock(TransactionService)

    def coinInformationService = new CoinInformationService(pricingFacade, holdingService, calculateAmountSpent, transactionService)


    def "should return null if all transactions are already processed"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "Test")
        def transactions = [
                new Transaction(symbol: symbol, side: "BUY", executed: 1, paidWith: "USDT", paidAmount: 100, processed: true, portfolio: portfolio)
        ]

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, transactions)

        then:
        result == null
    }

    def "should calculate cost in USDT when paidWith is non-stable"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "Test")
        def transaction = new Transaction(symbol: symbol, side: "BUY", executed: 1, paidWith: "BTC", paidAmount: 0.01, portfolio: portfolio)
        pricingFacade.getCurrentMarketPrice(_) >> 20000
        holdingService.getOrCreateByPortfolioAndSymbol(_, _) >> new Holding(symbol: symbol, portfolio: portfolio)

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, [transaction])

        then:
        result.stableTotalCost == 20000
        result.amount == 1

        and:
        1 * pricingFacade.getPriceInUsdt(_, _) >> 20000
    }

    def "should warn if attempting to sell more than held"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "Test")
        def transactions = [
                new Transaction(symbol: symbol, side: "BUY", executed: 1, paidWith: "USDT", paidAmount: 100, portfolio: portfolio),
                new Transaction(symbol: symbol, side: "SELL", executed: 2, paidWith: "USDT", paidAmount: 200, portfolio: portfolio)
        ]
        pricingFacade.getCurrentMarketPrice(_) >> 100
        holdingService.getOrCreateByPortfolioAndSymbol(_, _) >> new Holding(symbol: symbol, portfolio: portfolio)

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, transactions)

        then:
        result.amount == 0
        result.realizedProfit == 200
    }

    def "should correctly update holding fields including amountInUsdt"() {
        given:
        def symbol = "ETH"
        def portfolio = new Portfolio(name: "Test")
        def initialAmount = 2.5
        def currentPrice = 3000.0
        def expectedAmountInUsdt = initialAmount * currentPrice

        // Create a transaction that will result in the holding having the initialAmount
        def transaction = new Transaction(
            symbol: symbol, 
            side: "BUY", 
            executed: initialAmount, 
            paidWith: "USDT", 
            paidAmount: 2500.0, 
            portfolio: portfolio
        )

        // Mock the holding that will be updated
        def holding = new Holding(
            symbol: symbol,
            portfolio: portfolio,
            amount: BigDecimal.ZERO,
            amountInUsdt: BigDecimal.ZERO,
            totalAmountBought: BigDecimal.ZERO,
            totalAmountSold: BigDecimal.ZERO,
            stableTotalCost: BigDecimal.ZERO,
            currentPositionInUsdt: BigDecimal.ZERO,
            totalRealizedProfitUsdt: BigDecimal.ZERO
        )

        // Mock dependencies
        pricingFacade.getCurrentMarketPrice(_) >> currentPrice
        holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol) >> holding

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, [transaction])

        then:
        // Verify the holding was saved with the correct values
        1 * holdingService.save({ Holding savedHolding ->
            savedHolding.amount == initialAmount &&
            savedHolding.amountInUsdt.setScale(1, BigDecimal.ROUND_HALF_UP) == expectedAmountInUsdt.setScale(1, BigDecimal.ROUND_HALF_UP) &&
            savedHolding.totalAmountBought == initialAmount &&
            savedHolding.currentPositionInUsdt == expectedAmountInUsdt
        })

        // Verify the response has the correct values
        result.amount == initialAmount
        result.currentPrice == currentPrice
        result.currentPositionInUsdt == expectedAmountInUsdt
    }

    def "should return empty response when transactions list is empty"() {
        given:
        def symbol = "ETH"
        def transactions = []

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, transactions)

        then:
        result.getCoinName() == symbol
        result.getAmount() == BigDecimal.ZERO
        result.getStableTotalCost() == BigDecimal.ZERO
        result.getTotalTransactions() == 0
    }
}
