package com.importer.fileimporter.service

import com.importer.fileimporter.dto.HoldingDto
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Symbol
import com.importer.fileimporter.repository.HoldingRepository
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class HoldingServiceSpec extends Specification {

    HoldingRepository holdingRepository = Mock(HoldingRepository)
    HoldingService holdingService = new HoldingService(holdingRepository)

    def "test saveSymbolHolding - existing holding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def symbol = new Symbol(symbol: "BTC")
        def amount = new BigDecimal("100")
        def existingHolding = Holding.builder()
                .id(UUID.randomUUID())
                .symbol("BTC")
                .portfolio(portfolio)
                .amount(new BigDecimal("50"))
                .build()

        when:
        def result = holdingService.saveSymbolHolding(symbol, portfolio, amount)

        then:
        result.amount == amount
        result.symbol == "BTC"

        and:
        1 * holdingRepository.findBySymbolAndPortfolioName("BTC", "Test Portfolio") >> Optional.of(existingHolding)
        1 * holdingRepository.save(_) >> { Holding h ->
            assert h.amount == amount
            assert h.symbol == "BTC"
            assert h.portfolio.name == "Test Portfolio"
            assert h.modified != null
            assert h.modifiedBy == "Modifying holding"
            h
        }
    }

    def "test saveSymbolHolding - new holding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def symbol = new Symbol(symbol: "ETH")
        def amount = new BigDecimal("200")

        when:
        def result = holdingService.saveSymbolHolding(symbol, portfolio, amount)

        then:
        result.amount == amount
        result.symbol == "ETH"

        and:
        1 * holdingRepository.findBySymbolAndPortfolioName("ETH", "Test Portfolio") >> Optional.empty()
        1 * holdingRepository.save(_) >> { Holding h ->
            assert h.amount == amount
            assert h.symbol == "ETH"
            assert h.portfolio.name == "Test Portfolio"
            assert h.created != null
            assert h.createdBy == "Adding holding"
            assert h.modified != null
            assert h.modifiedBy == "Adding holding"
            h
        }
    }

    def "test getBySymbolAndPortfolioName"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def symbol = "BTC"
        def holding = Optional.of(new Holding(symbol: "BTC", portfolio: portfolio))

        when:
        def result = holdingService.getByPortfolioAndSymbol(portfolio, symbol)

        then:
        1 * holdingRepository.findBySymbolAndPortfolioName(symbol, "Test Portfolio") >> holding

        and:
        result == holding
    }

    def "test getByPortfolio"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def holdings = [new Holding(symbol: "BTC", portfolio: portfolio),
                        new Holding(symbol: "ETH", portfolio: portfolio)]

        when:
        def result = holdingService.getByPortfolio(portfolio)

        then:
        1 * holdingRepository.findAllByPortfolio(portfolio) >> holdings

        and:
        result == holdings
    }

    def "test updatePercentageHolding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def holdingDto = HoldingDto.builder()
                .symbol("BTC")
                .percentage(new BigDecimal("50"))
                .priceInUsdt(new BigDecimal("30000"))
                .priceInBtc(new BigDecimal("1"))
                .amountInBtc(new BigDecimal("0.5"))
                .amountInUsdt(new BigDecimal("15000"))
                .build()
        def existingHolding = new Holding(symbol: "BTC", portfolio: portfolio, percent: new BigDecimal("20"))

        when:
        def result = holdingService.updatePercentageHolding(holdingDto, portfolio)

        then:
        1 * holdingRepository.findBySymbolAndPortfolioName("BTC", "Test Portfolio") >> Optional.of(existingHolding)
        1 * holdingRepository.save(_) >> { Holding h -> h }

        and:
        result.symbol == "BTC"
        result.percentage == new BigDecimal("50")
        result.priceInUsdt == new BigDecimal("30000")
        result.priceInBtc == new BigDecimal("1")
        result.amountInBtc == new BigDecimal("0.5")
        result.amountInUsdt == new BigDecimal("15000")
    }

    def "test getHolding"() {
        given:
        def symbol = "BTC"
        def holding = new Holding(symbol: "BTC")

        when:
        def result = holdingService.getHolding(new Portfolio(), symbol)

        then:
        1 * holdingRepository.findByPortfolioAndSymbol(_, symbol) >> Optional.of(holding)
        result == holding
    }

    def "test getHolding - symbol not found"() {
        given:
        def symbol = "BTC"

        when:
        holdingService.getHolding(new Portfolio(), symbol)

        then:
        1 * holdingRepository.findByPortfolioAndSymbol(_ as Portfolio, symbol) >> Optional.empty()
        thrown(ResponseStatusException)
    }
    def "test updatePaidWithHolding - buy transaction"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def paidWithSymbol = "BTC"
        def paidAmount = new BigDecimal("0.1")
        def executed = new BigDecimal("1")
        def paidInStable = new BigDecimal("1000")
        def existingHolding = Holding.builder()
                .symbol(paidWithSymbol)
                .portfolio(portfolio)
                .amount(new BigDecimal("1"))
                .totalAmountSold(BigDecimal.ZERO)
                .build()

        when:
        holdingService.updatePaidWithHolding(true, paidWithSymbol, paidAmount, portfolio, executed, paidInStable)

        then:
        1 * holdingRepository.findBySymbolAndPortfolioName(paidWithSymbol, "Test Portfolio") >> Optional.of(existingHolding)
        1 * holdingRepository.save(_) >> { Holding h ->
            assert h.amount == new BigDecimal("1.1") // 1 + 0.1
            assert h.totalAmountSold == BigDecimal.ZERO // Unchanged for buy
            h
        }
    }

    def "test updatePaidWithHolding - sell transaction"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def paidWithSymbol = "BTC"
        def paidAmount = new BigDecimal("0.1")
        def executed = new BigDecimal("1")
        def paidInStable = new BigDecimal("1000")
        def existingHolding = Holding.builder()
                .symbol(paidWithSymbol)
                .portfolio(portfolio)
                .amount(new BigDecimal("1"))
                .totalAmountSold(new BigDecimal("0.5"))
                .build()

        when:
        holdingService.updatePaidWithHolding(false, paidWithSymbol, paidAmount, portfolio, executed, paidInStable)

        then:
        1 * holdingRepository.findBySymbolAndPortfolioName(paidWithSymbol, "Test Portfolio") >> Optional.of(existingHolding)
        1 * holdingRepository.save(_) >> { Holding h ->
            assert h.amount == new BigDecimal("0.9") // 1 - 0.1
            assert h.totalAmountSold == new BigDecimal("0.6") // 0.5 + 0.1
            h
        }
    }

    def "test updatePaidWithHolding - new holding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def paidWithSymbol = "ETH"
        def paidAmount = new BigDecimal("2")
        def executed = new BigDecimal("1")
        def paidInStable = new BigDecimal("2000")
        def newHolding = Holding.builder()
                .symbol(paidWithSymbol)
                .portfolio(portfolio)
                .amount(BigDecimal.ZERO)
                .totalAmountSold(BigDecimal.ZERO)
                .build()

        when:
        holdingService.updatePaidWithHolding(true, paidWithSymbol, paidAmount, portfolio, executed, paidInStable)

        then:
        1 * holdingRepository.findBySymbolAndPortfolioName(paidWithSymbol, "Test Portfolio") >> Optional.empty()
        1 * holdingRepository.save(_) >> { Holding h ->
            assert h.amount == new BigDecimal("2") // 0 + 2
            assert h.totalAmountSold == BigDecimal.ZERO // Unchanged for buy
            h
        }
    }
}
