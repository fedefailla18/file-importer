package com.importer.fileimporter.facade

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import spock.lang.Subject

import java.time.LocalDateTime

class CoinInformationFacadeIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    CalculateAmountSpent calculateAmountSpent

    @Autowired
    PricingFacade pricingFacade

    @Autowired
    TransactionService transactionService

    @Autowired
    @Subject
    CoinInformationFacade coinInformationFacade

    def "test getTransactionsInformation with predefined transactions"() {
        given: "a set of predefined transactions"
        def symbol = "BAND"

        def transactions = getTransactions()

        Page<Transaction> transactionPage = new PageImpl<>(transactions)

        when: "getTransactionsInformation is called"
        def response = coinInformationFacade.getTransactionsInformation(symbol)

        then: "the response contains the correct information"
        response.coinName == symbol
        response.totalTransactions == 5
        response.amount == transactions.stream()
                .map { t -> OperationUtils.sumAmount(BigDecimal.ZERO, t.getTransactionId().getExecuted(), t.getTransactionId().getSide()) }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        and: 'Validate average entry price calculation'
        def totalSpent = transactions.stream()
                .map { t -> calculateAmountSpent.getAmountSpentInUsdtPerTransaction(symbol, t, response) }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        def totalAmount = transactions.stream()
                .map { t -> t.getTransactionId().getExecuted() }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        response.stableTotalCost == totalSpent
        response.avgEntryPrice[USDT] == totalAmount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : totalSpent.divide(totalAmount, RoundingMode.HALF_UP)

//        and:
//        1 * transactionService.getTransactionsByRangeDate(_, _, _, _) >> transactionPage
    }

    ArrayList<Transaction> getTransactions(symbol) {
        return [
                createTransaction(symbol, "BTC", "BUY", 82.5, 0.0000618000),
                createTransaction(symbol, "BTC", "BUY", 55.0, 0.0000936),
                createTransaction(symbol, "BTC", "SELL", 76.2, 0.0001311000),
                createTransaction(symbol, "USDT", "BUY", 23.6, 1.209),
                createTransaction(symbol, "USDT", "SELL", 56.0, 1.79),
                createTransaction(symbol, "USDT", "BUY", 45.6, 1.209)
        ]
    }

    private Transaction createTransaction(String symbol, String payedWith, String side, BigDecimal executed, BigDecimal price) {
        def payedAmount = executed * price
        def transactionId = new TransactionId(
                executed: executed,
                side: side,
                dateUtc: LocalDateTime.now(),
                price: price
        )
        new Transaction(
                symbol: symbol,
                payedWith: payedWith,
                transactionId: transactionId,
                payedAmount: payedAmount
        )
    }
}
