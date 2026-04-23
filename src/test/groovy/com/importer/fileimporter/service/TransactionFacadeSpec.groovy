package com.importer.fileimporter.service

import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.TransactionProcessor
import com.importer.fileimporter.dto.HoldingDto
import com.importer.fileimporter.dto.TransactionHoldingDto
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.RoundingMode
import spock.lang.Specification

import java.time.LocalDateTime

class TransactionFacadeSpec extends Specification {

    def transactionService = Mock(TransactionService)
    def pricingFacade = Mock(PricingFacade)
    def transactionProcessor = Mock(TransactionProcessor)
    def holdingService = Mock(HoldingService)
    def portfolioService = Mock(PortfolioService)

    def sut = new TransactionFacade(transactionService, pricingFacade, transactionProcessor, holdingService, portfolioService)

    def "BuildPortfolio should correctly return holdings from HoldingService"() {
        given: "Mock data for holdings"
        def symbols = ["BTC"]
        def portfolio = new Portfolio(name: "Binance")
        def holding = new Holding(
                symbol: "BTC",
                amount: BigDecimal.valueOf(50),
                stableTotalCost: BigDecimal.valueOf(5000),
                totalRealizedProfitUsdt: BigDecimal.valueOf(1000),
                percent: BigDecimal.valueOf(10),
                portfolio: portfolio
        )

        and:
        portfolioService.findAll() >> [portfolio]
        holdingService.getByPortfolio(portfolio) >> [holding]
        pricingFacade.getPrices("BTC") >> [BTC: 0.00015d, USDT: 1.5d]

        when: "Building the portfolio"
        List<TransactionHoldingDto> result = sut.buildPortfolio([])

        then: "The result is correctly mapped from holdings"
        result.size() == 1
        with(result[0]) {
            symbol == "BTC"
            amount == BigDecimal.valueOf(50)
            stableTotalCost == BigDecimal.valueOf(5000)
            totalRealizedProfitUsdt == BigDecimal.valueOf(1000)
            priceInBtc == BigDecimal.valueOf(0.00015)
            priceInUsdt == BigDecimal.valueOf(1.5)
            amountInBtc == (amount * priceInBtc).setScale(8, RoundingMode.HALF_UP)
            amountInUsdt == (amount * priceInUsdt).setScale(8, RoundingMode.HALF_UP)
        }
    }

    def "GetAmount should handle symbols list"() {
        given: "A list of symbols"
        def symbols = ["BTC"]
        def holdingDto = HoldingDto.builder()
                .symbol("BTC")
                .amount(BigDecimal.valueOf(50))
                .stableTotalCost(BigDecimal.valueOf(5000))
                .totalRealizedProfitUsdt(BigDecimal.valueOf(1000))
                .percentage(BigDecimal.valueOf(10))
                .build()

        and:
        holdingService.getBySymbol("BTC") >> [holdingDto]
        pricingFacade.getPrices("BTC") >> [BTC: 0.00015d, USDT: 1.5d]

        when: "Getting amount for symbols"
        def result = sut.getAmount(symbols)

        then: "Result is correct"
        result.size() == 1
        result[0].symbol == "BTC"
        result[0].amount == BigDecimal.valueOf(50)
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
