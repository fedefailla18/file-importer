package com.importer.fileimporter.service

import com.importer.fileimporter.dto.TransactionHoldingDto
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import java.time.LocalDateTime

class TransactionFacadeSpec extends Specification {

    def transactionService = Mock(TransactionService)
    def calculateAmountSpent = Mock(CalculateAmountSpent)
    def coinInformationService = Mock(CoinInformationService)
    def symbolService = Mock(SymbolService)
    def portfolioService = Mock(PortfolioService)
    def getSymbolHistoricPriceService = Mock(GetSymbolHistoricPriceService)
    def pricingFacade = Mock(PricingFacade)

    def sut = new TransactionFacade(transactionService, calculateAmountSpent,
            coinInformationService, symbolService, portfolioService, getSymbolHistoricPriceService, pricingFacade)

    def "BuildPortfolio should correctly calculate holdings"() {
        given: "Mock data for transactions and symbols"
        def symbols = ["BAND"]
        def transactions = createMockTransactions()

        and:
        Page<Transaction> pageMock = new PageImpl<>(transactions)

        when: "Building the portfolio"
        List<TransactionHoldingDto> portfolio = sut.buildPortfolio(symbols)

        then: "The portfolio is correctly built with expected values"
        portfolio.size() == 1
        with(portfolio[0]) {
            symbol == "BAND"
            amount == BigDecimal.valueOf(50)
            buyPrice == BigDecimal.valueOf(1.0572)  // Example calculation, adjust as needed
            buyPriceInBtc == BigDecimal.valueOf(0.00015858) // Example calculation, adjust as needed
            sellPrice == BigDecimal.valueOf(1.7818)  // Example calculation, adjust as needed
            sellPriceInBtc == BigDecimal.valueOf(0.00026727) // Example calculation, adjust as needed
            payedInUsdt == BigDecimal.valueOf(17.975129) // Example calculation, adjust as needed
            payedInBtc == BigDecimal.valueOf(0.02696325) // Example calculation, adjust as needed
            priceInBtc == BigDecimal.valueOf(0.00015)
            priceInUsdt == BigDecimal.valueOf(1.5)
            amountInBtc == BigDecimal.valueOf(0.00735)  // amount (49) * price in BTC (0.00015)
            amountInUsdt == BigDecimal.valueOf(73.5)  // amount (49) * price in USDT (1.5)
        }

        and:
        1 * transactionService.getAllBySymbol("BAND", Pageable.unpaged()) >> pageMock

        and: 'Mock the pricing facade to return prices'
        pricingFacade.getPrice("BAND", "USDT", _ as LocalDateTime) >> BigDecimal.valueOf(1.5)
        pricingFacade.getPrice("BAND", "BTC", _ as LocalDateTime) >> BigDecimal.valueOf(0.00015)
        pricingFacade.getPrices("BAND") >> [BTC: 0.00015d, USDT: 1.5d]
    }

    def "GetTotalAmount"() {
        given:
        def transactions = createMockTransactions()

        when:
        def totalAmount = sut.getTotalAmount(transactions).get()

        then:
        totalAmount == BigDecimal.valueOf(50)
    }

    public static List<Transaction> createMockTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        def transactionId1 = TransactionId.builder()
                .dateUtc(LocalDateTime.now())
                .side('BUY').pair("BTCUSD")
                .executed(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(50))
                .build()
        // Create mock transactions
        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .transactionId(transactionId1)
                .symbol("BTC")
                .payedWith("USD")
                .payedAmount(BigDecimal.valueOf(5000))
                .fee("5USD")
                .feeAmount(BigDecimal.valueOf(5))
                .feeSymbol("USD")
                .created(LocalDateTime.now())
                .createdBy("user1")
                .modified(LocalDateTime.now())
                .modifiedBy("user1")
                .build();

        def transactionId2 = TransactionId.builder()
                .dateUtc(LocalDateTime.now())
                .side('SELL')
                .pair("BTCUSD")
                .executed(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(100))
                .build()
        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .transactionId(transactionId2)
                .symbol("BTC")
                .payedWith("USD")
                .payedAmount(BigDecimal.valueOf(5000))
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
