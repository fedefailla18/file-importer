package com.importer.fileimporter.service

import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import spock.lang.Specification

class CoinInformationServiceSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def transactionProcessor = Mock(TransactionProcessor)

    def coinInformationService = new CoinInformationService(pricingFacade, holdingService, transactionProcessor)


    def "should return response even if all transactions are already processed"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "Test")
        def transactions = [
                new Transaction(symbol: symbol, side: "BUY", executed: 1, paidWith: "USDT", paidAmount: 100, processed: true, portfolio: portfolio)
        ]
        holdingService.getHolding(portfolio, symbol) >> new Holding(symbol: symbol, portfolio: portfolio, amount: 1, stableTotalCost: 100)
        pricingFacade.getCurrentMarketPrice(symbol) >> 150

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, transactions)

        then:
        result != null
        result.coinName == symbol
        result.amount == 1
    }

    def "should calculate cost in USDT when paidWith is non-stable"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "Test")
        def transaction = new Transaction(symbol: symbol, side: "BUY", executed: 1, paidWith: "BTC", paidAmount: 0.01, portfolio: portfolio)
        pricingFacade.getCurrentMarketPrice(_) >> 100
        holdingService.getHolding(_, _) >> new Holding(symbol: symbol, portfolio: portfolio, stableTotalCost: 20000, amount: 1)

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
        holdingService.getHolding(_, _) >> new Holding(symbol: symbol, portfolio: portfolio, amount: 0, totalRealizedProfitUsdt: -100)

        when:
        def result = coinInformationService.getCoinInformationResponse(symbol, transactions)

        then:
        result.amount == 0
        result.totalRealizedProfitUsdt < 0
    }
}
