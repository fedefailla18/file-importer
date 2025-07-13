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
        pricingFacade.getPriceInUsdt(_, _) >> 20000
        holdingService.getOrCreateByPortfolioAndSymbol(_, _) >> new Holding(symbol: symbol, portfolio: portfolio)

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, [transaction])

        then:
        result.stableTotalCost == 20000
        result.amount == 1
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
        result.realizedProfit < 0
    }
}
