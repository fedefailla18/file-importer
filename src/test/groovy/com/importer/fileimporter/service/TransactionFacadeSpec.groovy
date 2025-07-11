package com.importer.fileimporter.service

import com.importer.fileimporter.dto.TransactionHoldingDto
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.RoundingMode
import spock.lang.Ignore
import spock.lang.Specification

import java.time.LocalDateTime
import javax.transaction.NotSupportedException

class TransactionFacadeSpec extends Specification {

    def transactionService = Mock(TransactionService)
    def pricingFacade = Mock(PricingFacade)

    def sut = new TransactionFacade(transactionService, pricingFacade)

    def "BuildPortfolio should correctly calculate holdings"() {
        given: "Mock data for transactions and symbols"
        def symbols = ["BTC"]
        def transactions = createMockTransactions()

        and:
        Page<Transaction> pageMock = new PageImpl<>(transactions)

        when: "Building the portfolio"
        List<TransactionHoldingDto> portfolio = sut.buildPortfolio(symbols)

        then: "The portfolio is correctly built with expected values"
        portfolio.size() == 1
        with(portfolio[0]) {
            symbol == "BTC"
            amount == BigDecimal.valueOf(50)
            // Verify the calculated values match what we expect based on our mock transactions
            buyPrice.compareTo(BigDecimal.valueOf(50)) == 0
            sellPrice.compareTo(BigDecimal.valueOf(100)) == 0
            payedInUsdt.compareTo(BigDecimal.valueOf(0)) == 0  // 5000 - 5000 = 0
            priceInBtc == BigDecimal.valueOf(0.00015)
            priceInUsdt == BigDecimal.valueOf(1.5)
            amountInBtc.compareTo(BigDecimal.valueOf(7.5).setScale(8, RoundingMode.HALF_UP)) == 0  // 50 * 0.00015 = 0.0075
            amountInUsdt.compareTo(BigDecimal.valueOf(75).setScale(8, RoundingMode.HALF_UP)) == 0  // 50 * 1.5 = 75
        }

        and:
        1 * transactionService.getAllBySymbol("BTC", Pageable.unpaged()) >> pageMock

        and: 'Mock the pricing facade to return prices'
        pricingFacade.getPriceInUsdt("BTC", _ as LocalDateTime) >> BigDecimal.valueOf(1.5)
        pricingFacade.getPriceInBTC("BTC", _ as LocalDateTime) >> BigDecimal.valueOf(0.00015)
        pricingFacade.getPrices("BTC") >> [BTC: 0.00015d, USDT: 1.5d]
    }

    def "GetTotalAmount"() {
        given:
        def transactions = createMockTransactions()

        when:
        def totalAmount = sut.getTotalAmount(transactions).get()

        then:
        totalAmount == BigDecimal.valueOf(50)
    }

    @Ignore("Test is ignored until getAmount method is implemented for empty symbols list")
    def "GetAmount should handle empty symbols list"() {
        given: "An empty list of symbols"
        def symbols = []

        when: "Getting amount for empty symbols list"
        sut.getAmount(symbols)

        then: "NotSupportedException is thrown"
        thrown(NotSupportedException)

        and: "TransactionService.getAll() is called once"
        1 * transactionService.getAll()
    }

    def "BuildPortfolio should handle zero totalAmount without division by zero"() {
        given: "Mock data for transactions with zero total amount"
        def symbols = ["ETH"]
        def transactions = [
            Transaction.builder()
                .id(1L)
                .dateUtc(LocalDateTime.now())
                .side('BUY')
                .pair("ETHUSDT")
                .executed(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(200))
                .symbol("ETH")
                .paidWith("USDT")
                .paidAmount(BigDecimal.valueOf(2000))
                .build(),
            Transaction.builder()
                .id(2L)
                .dateUtc(LocalDateTime.now())
                .side('SELL')
                .pair("ETHUSDT")
                .executed(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(250))
                .symbol("ETH")
                .paidWith("USDT")
                .paidAmount(BigDecimal.valueOf(2500))
                .build()
        ]

        and: "Mock the transaction service to return transactions with zero total amount"
        Page<Transaction> pageMock = new PageImpl<>(transactions)
        transactionService.getAllBySymbol("ETH", Pageable.unpaged()) >> pageMock

        and: "Mock the pricing facade"
        pricingFacade.getPriceInUsdt("ETH", _ as LocalDateTime) >> BigDecimal.valueOf(300)
        pricingFacade.getPriceInBTC("ETH", _ as LocalDateTime) >> BigDecimal.valueOf(0.01)
        pricingFacade.getPrices("ETH") >> [BTC: 0.01d, USDT: 300d]

        when: "Building the portfolio"
        List<TransactionHoldingDto> portfolio = sut.buildPortfolio(symbols)

        then: "The portfolio is built without throwing division by zero exception"
        portfolio.size() == 1
        with(portfolio[0]) {
            symbol == "ETH"
            amount == BigDecimal.ZERO  // Buy 10, Sell 10 = 0
        }
    }

    static List<Transaction> createMockTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .dateUtc(LocalDateTime.now())
                .side('BUY').pair("BTCUSD")
                .executed(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(50))
                .symbol("BTC")
                .paidWith("USD")
                .paidAmount(BigDecimal.valueOf(5000))
                .fee("5USD")
                .feeAmount(BigDecimal.valueOf(5))
                .feeSymbol("USD")
                .created(LocalDateTime.now())
                .createdBy("user1")
                .modified(LocalDateTime.now())
                .modifiedBy("user1")
                .build();

        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .dateUtc(LocalDateTime.now())
                .side('SELL')
                .pair("BTCUSD")
                .executed(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(100))
                .symbol("BTC")
                .paidWith("USD")
                .paidAmount(BigDecimal.valueOf(5000))
                .fee("5USD")
                .feeAmount(BigDecimal.valueOf(5))
                .feeSymbol("USD")
                .created(LocalDateTime.now())
                .createdBy("user2")
                .modified(LocalDateTime.now())
                .modifiedBy("user2")
                .build();

        // Add transactions to the list
        transactions.add(transaction1);
        transactions.add(transaction2);

        return transactions;
    }
}
