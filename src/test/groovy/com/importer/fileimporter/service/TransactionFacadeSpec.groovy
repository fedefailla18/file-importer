package com.importer.fileimporter.service

import com.importer.fileimporter.dto.TransactionDto
import com.importer.fileimporter.dto.TransactionHoldingDto
import com.importer.fileimporter.entity.Portfolio
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
    def holdingService = Mock(HoldingService)
    def portfolioService = Mock(PortfolioService)

    def sut = new TransactionFacade(transactionService, pricingFacade, holdingService, portfolioService)

    

    def "GetTotalAmount"() {
        given:
        def transactions = createMockTransactions()

        when:
        def totalAmount = sut.getTotalAmount(transactions).get()

        then:
        totalAmount == BigDecimal.valueOf(50)
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

    def "Save should assign portfolio when portfolioName is provided"() {
        given: "A transaction DTO with portfolio name"
        def portfolioName = "TestPortfolio"
        def transactionDto = TransactionDto.builder()
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.5))
                .dateUtc(LocalDateTime.now())
                .portfolioName(portfolioName)
                .build()

        and: "A portfolio that will be returned by the service"
        def portfolio = Portfolio.builder()
                .name(portfolioName)
                .creationDate(LocalDateTime.now())
                .build()

        and: "A transaction that will be created from the DTO"
        def transaction = Transaction.builder()
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.5))
                .dateUtc(LocalDateTime.now())
                .build()

        and: "A saved transaction with portfolio"
        def savedTransaction = Transaction.builder()
                .id(1L)
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.5))
                .dateUtc(LocalDateTime.now())
                .portfolio(portfolio)
                .build()

        and: "A mock for pricing facade"
        pricingFacade.getPriceInUsdt(_, _) >> BigDecimal.valueOf(300)

        when: "The save method is called"
        def result = sut.save(transactionDto)

        then: "The portfolio service is called to find or save the portfolio"
        1 * portfolioService.findOrSave(portfolioName) >> portfolio

        and: "The transaction is saved with the portfolio"
        1 * transactionService.save({ Transaction t ->
            t.portfolio == portfolio && t.symbol == "ETH"
        }) >> savedTransaction

        and: "The result has the correct portfolio"
        result.portfolio == portfolio
        result.symbol == "ETH"
    }

    def "Save should use default portfolio when portfolioName is not provided"() {
        given: "A transaction DTO without portfolio name"
        def transactionDto = TransactionDto.builder()
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.5))
                .dateUtc(LocalDateTime.now())
                .build()

        and: "A default portfolio that will be returned by the service"
        def defaultPortfolio = Portfolio.builder()
                .name("Default")
                .creationDate(LocalDateTime.now())
                .build()

        and: "A transaction that will be created from the DTO"
        def transaction = Transaction.builder()
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.5))
                .dateUtc(LocalDateTime.now())
                .build()

        and: "A saved transaction with default portfolio"
        def savedTransaction = Transaction.builder()
                .id(1L)
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.5))
                .dateUtc(LocalDateTime.now())
                .portfolio(defaultPortfolio)
                .build()

        and: "A mock for pricing facade"
        pricingFacade.getPriceInUsdt(_, _) >> BigDecimal.valueOf(300)

        when: "The save method is called"
        def result = sut.save(transactionDto)

        then: "The portfolio service is called to find or save the default portfolio"
        1 * portfolioService.findOrSave("Default") >> defaultPortfolio

        and: "The transaction is saved with the default portfolio"
        1 * transactionService.save({ Transaction t ->
            t.portfolio == defaultPortfolio && t.symbol == "ETH"
        }) >> savedTransaction

        and: "The result has the default portfolio"
        result.portfolio == defaultPortfolio
        result.symbol == "ETH"
    }

    def "Save should fetch price from pricingFacade when price is missing or not positive"() {
        given: "A transaction DTO without price, pair, or paidWith"
        def transactionDto = TransactionDto.builder()
                .symbol("BTC")
                .executed(BigDecimal.valueOf(2.0))
                .side("BUY")
                .dateUtc(LocalDateTime.now())
                .portfolioName("MyPortfolio")
                .build()

        and: "Mocks for pricing and portfolio"
        def portfolio = Portfolio.builder().name("MyPortfolio").build()
        def priceFromFacade = BigDecimal.valueOf(30000)
        pricingFacade.getPriceInUsdt("BTC", _ as LocalDateTime) >> priceFromFacade
        portfolioService.findOrSave("MyPortfolio") >> portfolio

        and: "Capture the saved transaction"
        Transaction savedTransaction = null
        transactionService.save(_ as Transaction) >> { Transaction t -> savedTransaction = t; return t }

        when: "Calling save"
        def result = sut.save(transactionDto)

        then: "The price is fetched from pricingFacade"
        result.price == priceFromFacade
        result.paidWith == "USDT"
        result.paidAmount == BigDecimal.valueOf(2.0).multiply(priceFromFacade)

        and: "Portfolio is correctly assigned"
        result.portfolio.name == "MyPortfolio"

        and: "Saved transaction has correct values"
        savedTransaction.symbol == "BTC"
        savedTransaction.price == priceFromFacade
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
