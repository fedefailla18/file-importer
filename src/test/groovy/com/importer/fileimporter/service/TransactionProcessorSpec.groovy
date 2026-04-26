package com.importer.fileimporter.service

import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import spock.lang.Specification

import java.time.LocalDateTime

class TransactionProcessorSpec extends Specification {

    def transactionService = Mock(TransactionService)
    def holdingService = Mock(HoldingService)
    def pricingFacade = Mock(PricingFacade)
    def portfolioService = Mock(PortfolioService)
    TransactionProcessor transactionProcessor = new TransactionProcessor(transactionService, holdingService, pricingFacade, portfolioService)

    def "should process BUY transaction and update cost basis"() {
        given:
        Portfolio portfolio = Portfolio.builder().name("TestPortfolio").build()
        Transaction transaction = Transaction.builder()
                .symbol("BTC")
                .side("BUY")
                .executed(new BigDecimal("0.5"))
                .price(new BigDecimal("20000"))
                .paidWith("USDT")
                .portfolio(portfolio)
                .dateUtc(LocalDateTime.now())
                .build()
        
        Holding holding = Holding.builder()
                .symbol("BTC")
                .portfolio(portfolio)
                .amount(BigDecimal.ZERO)
                .stableTotalCost(BigDecimal.ZERO)
                .build()

        when:
        transactionProcessor.process(transaction)

        then:
        1 * portfolioService.findOrSave("TestPortfolio") >> portfolio
        (1..2) * transactionService.save(_) >> transaction
        1 * holdingService.getHolding(portfolio, "BTC") >> holding
        1 * holdingService.save({ Holding h -> 
            h.amount == new BigDecimal("0.5") && 
            h.stableTotalCost == new BigDecimal("10000.00") // 0.5 * 20000
        })
    }

    def "should process SELL transaction and calculate realized profit"() {
        given:
        Portfolio portfolio = Portfolio.builder().name("TestPortfolio").build()
        
        // Initial state: 1 BTC at 20000 USDT cost
        Holding holding = Holding.builder()
                .symbol("BTC")
                .portfolio(portfolio)
                .amount(new BigDecimal("1.0"))
                .stableTotalCost(new BigDecimal("20000"))
                .totalRealizedProfitUsdt(BigDecimal.ZERO)
                .build()

        // Sell 0.4 BTC at 40000 USDT
        Transaction transaction = Transaction.builder()
                .symbol("BTC")
                .side("SELL")
                .executed(new BigDecimal("0.4"))
                .price(new BigDecimal("40000"))
                .paidWith("USDT")
                .portfolio(portfolio)
                .dateUtc(LocalDateTime.now())
                .build()

        when:
        transactionProcessor.process(transaction)

        then:
        1 * portfolioService.findOrSave("TestPortfolio") >> portfolio
        (1..2) * transactionService.save(_) >> transaction
        1 * holdingService.getHolding(portfolio, "BTC") >> holding
        1 * holdingService.save({ Holding h -> 
            h.amount == new BigDecimal("0.6") && // 1.0 - 0.4
            h.stableTotalCost == new BigDecimal("12000.0000000000") && // 0.6 * 20000 (avg cost)
            h.totalRealizedProfitUsdt == new BigDecimal("8000.0000000000") // (40000 - 20000) * 0.4
        })
    }

    def "should process non-stable pair trade (BTC/ETH)"() {
        given:
        Portfolio portfolio = Portfolio.builder().name("TestPortfolio").build()
        
        // Initial state: 1 BTC, 0 ETH
        Holding btcHolding = Holding.builder()
                .symbol("BTC")
                .portfolio(portfolio)
                .amount(new BigDecimal("1.0"))
                .stableTotalCost(new BigDecimal("20000"))
                .build()
        
        Holding ethHolding = Holding.builder()
                .symbol("ETH")
                .portfolio(portfolio)
                .amount(BigDecimal.ZERO)
                .stableTotalCost(BigDecimal.ZERO)
                .build()

        // BUY 10 ETH with 0.5 BTC
        Transaction transaction = Transaction.builder()
                .symbol("ETH")
                .side("BUY")
                .executed(new BigDecimal("10"))
                .price(new BigDecimal("0.05"))
                .paidWith("BTC")
                .paidAmount(new BigDecimal("0.5"))
                .portfolio(portfolio)
                .dateUtc(LocalDateTime.now())
                .build()

        when:
        transactionProcessor.process(transaction)

        then:
        1 * portfolioService.findOrSave("TestPortfolio") >> portfolio
        (1..2) * transactionService.save(_) >> transaction
        
        // Update ETH holding (Primary)
        1 * holdingService.getHolding(portfolio, "ETH") >> ethHolding
        1 * pricingFacade.getPriceInUsdt("ETH", _) >> new BigDecimal("1000") // $1000 per ETH
        1 * holdingService.save({ Holding h -> h.symbol == "ETH" && h.amount == new BigDecimal("10") })
        
        // Update BTC holding (PaidWith - SELL)
        1 * holdingService.getHolding(portfolio, "BTC") >> btcHolding
        1 * pricingFacade.getPriceInUsdt("BTC", _) >> new BigDecimal("20000")
        1 * holdingService.save({ Holding h -> 
            h.symbol == "BTC" && 
            h.amount == new BigDecimal("0.5") && 
            h.stableTotalCost == new BigDecimal("10000.0000000000")
        })
    }
}
