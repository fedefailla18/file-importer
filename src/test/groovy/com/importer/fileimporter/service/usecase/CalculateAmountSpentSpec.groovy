package com.importer.fileimporter.service.usecase

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.service.HoldingService
import spock.lang.Specification

class CalculateAmountSpentSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def sut = new CalculateAmountSpent(pricingFacade, holdingService)

    def "GetAmountSpentInUsdtPerTransaction. Single BUY transaction"() {
        given:
        def transaction = new Transaction(
                        side: "BUY",
                        pair: "RLCUSDT",
                        price: 1,
                        executed: 200,
                symbol: "RLC",
                paidWith: "USDT",
                paidAmount: 200,
                feeAmount: 0.2
        )

        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = sut.getAmountSpentInUsdtPerTransaction(transaction.symbol, transaction, response, null)

        then:
        result == 200
        response.getStableTotalCost() == 200
        response.getSpent().get("USDT") == 200
    }

    def "GetAmountSpentInUsdtPerTransaction. Single SELL transaction"() {
        given:
        def transaction = new Transaction(
                        side: "SELL",
                        pair: "RLCUSDT",
                        price: 2.15,
                        executed: 70,
                symbol: "RLC",
                paidWith: "USDT",
                paidAmount: 150.78
        )
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = sut.getAmountSpentInUsdtPerTransaction(transaction.symbol, transaction, response, null)

        then:
        result == BigDecimal.valueOf(-150.78)
        response.totalRealizedProfitUsdt == BigDecimal.valueOf(150.78)
    }

    def "GetAmountSpentInUsdtPerTransaction. Multiple transactions"() {
        given:
        def transactions = [
                new Transaction(side: "BUY",
                                pair: "RLCUSDT",
                                price: 1,
                                executed: 200,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 200
                ),
                new Transaction(side: "SELL",
                                pair: "RLCUSDT",
                                price: 2.15,
                                executed: 70,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 150.78
                ),
                new Transaction(side: "SELL",
                                pair: "RLCUSDT",
                                price: 1.60,
                                executed: 50,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 91
                )
        ]
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        transactions.each { tx ->
            sut.getAmountSpentInUsdtPerTransaction(tx.symbol, tx, response, null)
        }

        then:
        response.spent.get("USDT") == 200
        response.stableTotalCost == 200
        response.totalRealizedProfitUsdt == 241.78
    }

    def "should return amount spent in USDT for a buy transaction paid with stable coin"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "USDT", "BUY", new BigDecimal("1"), new BigDecimal("500"))
        def response = CoinInformationResponse.createEmpty(symbol)

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == new BigDecimal("500")
        response.getSpent().get("USDT") == new BigDecimal("500")
        response.getStableTotalCost() == new BigDecimal("500")
    }

    def "should return amount spent in USDT for a buy transaction paid with non-stable coin"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "BTC", "BUY", new BigDecimal("1"), new BigDecimal("0.1"))
        def response = CoinInformationResponse.createEmpty(symbol)

        and:
        pricingFacade.getPriceInUsdt(symbol, _) >> 1000

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == 1000
        response.getSpent().get("BTC") == new BigDecimal("0.1")
        response.getStableTotalCost() == 1000
    }

    def "should return amount earned in USDT for a sell transaction"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "USDT", "SELL", new BigDecimal("1"), new BigDecimal("500"))
        def response = CoinInformationResponse.createEmpty(symbol)

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == new BigDecimal("-500")
        response.getSpent().isEmpty() // Spent should not include sell transactions
        response.getTotalRealizedProfitUsdt() == new BigDecimal("500")
    }

    def "should return amount earned in USDT for a sell transaction paid with non-stable coin"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "BTC", "SELL", new BigDecimal("1"), new BigDecimal("0.1"))
        def response = CoinInformationResponse.createEmpty(symbol)

        and:
        pricingFacade.getPriceInUsdt(symbol, _) >> 1000

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == -1000
        response.getSpent().isEmpty() // Spent should not include sell transactions
        response.totalRealizedProfitUsdt == 1000
    }

    private static Transaction createTransaction(String symbol, String paidWith, String side, BigDecimal executed, BigDecimal price) {
        BigDecimal paidAmount = executed.multiply(price)
        return new Transaction(
                symbol: symbol,
                paidWith: paidWith,
                executed: executed,
                side: side,
                price: price,
                paidAmount: paidAmount
        )
    }
}
