package com.importer.fileimporter.service.usecase

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.facade.PricingFacade
import spock.lang.Specification

class CalculateAmountSpentSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def service = new CalculateAmountSpent(pricingFacade)

    def "GetAmountSpentInUsdtPerTransaction. Single BUY transaction"() {
        given:
        def transaction = new Transaction(
                transactionId: new TransactionId(
                        side: "BUY",
                        pair: "RLCUSDT",
                        price: 1,
                        executed: 200
                ),
                symbol: "RLC",
                payedWith: "USDT",
                payedAmount: 200,
                feeAmount: 0.2
        )

        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = service.getAmountSpentInUsdtPerTransaction(transaction.symbol, transaction, response)

        then:
        result == 200
        response.getStableTotalCost() == 200
        response.getSpent().get("USDT") == 200
    }

    def "GetAmountSpentInUsdtPerTransaction. Single SELL transaction"() {
        given:
        def transaction = new Transaction(
                transactionId: new TransactionId(
                        side: "SELL",
                        pair: "RLCUSDT",
                        price: 2.15,
                        executed: 70
                ),
                symbol: "RLC",
                payedWith: "USDT",
                payedAmount: 150.78
        )
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = service.getAmountSpentInUsdtPerTransaction(transaction.symbol, transaction, response)

        then:
        result == BigDecimal.valueOf(-150.78)
        response.totalRealizedProfitUsdt == BigDecimal.valueOf(150.78)
    }

    def "GetAmountSpentInUsdtPerTransaction. Multiple transactions"() {
        given:
        def transactions = [
                new Transaction(
                        transactionId: new TransactionId(
                                side: "BUY",
                                pair: "RLCUSDT",
                                price: 1,
                                executed: 200
                        ),
                        symbol: "RLC",
                        payedWith: "USDT",
                        payedAmount: 200
                ),
                new Transaction(
                        transactionId: new TransactionId(
                                side: "SELL",
                                pair: "RLCUSDT",
                                price: 2.15,
                                executed: 70
                        ),
                        symbol: "RLC",
                        payedWith: "USDT",
                        payedAmount: 150.78
                ),
                new Transaction(
                        transactionId: new TransactionId(
                                side: "SELL",
                                pair: "RLCUSDT",
                                price: 1.60,
                                executed: 50
                        ),
                        symbol: "RLC",
                        payedWith: "USDT",
                        payedAmount: 91
                )
        ]
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        transactions.each { tx ->
            service.getAmountSpentInUsdtPerTransaction(tx.symbol, tx, response)
        }

        then:
        response.spent.get("USDT") == 200
        response.stableTotalCost == 200
        response.totalRealizedProfitUsdt == 241.78
    }
}
