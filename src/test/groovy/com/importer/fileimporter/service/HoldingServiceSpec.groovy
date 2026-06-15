package com.importer.fileimporter.service

import com.importer.fileimporter.dto.HoldingDto
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Symbol
import com.importer.fileimporter.repository.HoldingRepository
import spock.lang.Specification

class HoldingServiceSpec extends Specification {

    HoldingRepository holdingRepository = Mock(HoldingRepository)
    HoldingService holdingService = new HoldingService(holdingRepository)

    def "test saveSymbolHolding - existing holding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def symbol = new Symbol(symbol: "BTC")
        def amount = 100
        def existingHolding = Holding.builder()
                .id(UUID.randomUUID())
                .symbol("BTC")
                .portfolio(portfolio)
                .amount(50)
                .build()

        when:
        def result = holdingService.saveSymbolHolding(symbol, portfolio, amount)

        then:
        result.amount == amount
        result.symbol == "BTC"

        and:
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName("BTC", "Test Portfolio") >> Optional.of(existingHolding)
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
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName("ETH", "Test Portfolio") >> Optional.empty()
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
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName(symbol, "Test Portfolio") >> holding

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
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName("BTC", "Test Portfolio") >> Optional.of(existingHolding)
        1 * holdingRepository.save(_) >> { Holding h -> h }

        and:
        result.symbol == "BTC"
        result.percentage == new BigDecimal("50")
        result.priceInUsdt == new BigDecimal("30000")
        result.priceInBtc == new BigDecimal("1")
        result.amountInBtc == new BigDecimal("0.5")
        result.amountInUsdt == new BigDecimal("15000")
    }

    def "test resetHoldingsBySymbol"() {
        given:
        def symbol = "ADA"
        def holding1 = Holding.builder().symbol(symbol).amount(new BigDecimal("100")).build()
        def holding2 = Holding.builder().symbol(symbol).amount(new BigDecimal("200")).build()
        def holdings = [holding1, holding2]

        1 * holdingRepository.findAllBySymbolIgnoreCase(symbol) >> holdings
        // Capture the arguments saved for each holding
        def savedHoldings = []
        2 * holdingRepository.save(_) >> { Holding h ->
            savedHoldings << h
            h
        }

        when:
        def result = holdingService.resetHoldingsBySymbol(symbol)

        then:
        result == 2
        savedHoldings.size() == 2
        // Verify that all amounts were reset to zero for both holdings
        savedHoldings.every { it.amount == BigDecimal.ZERO }
        savedHoldings.every { it.amountInBtc == BigDecimal.ZERO }
        savedHoldings.every { it.amountInUsdt == BigDecimal.ZERO }
        savedHoldings.every { it.totalAmountBought == BigDecimal.ZERO }
        savedHoldings.every { it.totalAmountSold == BigDecimal.ZERO }
        savedHoldings.every { it.inventoryCostUsdt == BigDecimal.ZERO }
        savedHoldings.every { it.currentPositionInUsdt == BigDecimal.ZERO }
        savedHoldings.every { it.totalRealizedProfitUsdt == BigDecimal.ZERO }
        savedHoldings.every { it.modifiedBy.contains("resetHoldingsBySymbol") }
    }

    def """test updatePaidWithHolding logic - scenario: #scenario"""() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def paidWithSymbol = "USDT"
        def existingHolding = Holding.builder()
                .symbol(paidWithSymbol)
                .portfolio(portfolio)
                .amount(initialAmount)
                .totalAmountSold(initialSold)
                .build()

        when:
        holdingService.updatePaidWithHolding(isBuy, paidWithSymbol, paidAmount, portfolio)

        then:
        noExceptionThrown()


        and: "The getOrCreate method should return an existing holding or create a new one"
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName(paidWithSymbol, "Test Portfolio") >>
                (isNewHolding ? Optional.empty() : Optional.of(existingHolding))

        and: "we expect the save method to be called once, and we capture the holding to verify its state"
        1 * holdingRepository.save(_) >> { Holding h ->
            assert h.amount == expectedAmount
            assert h.totalAmountSold == expectedSold
            h
        }

        where:
        scenario                                                            | isBuy | isNewHolding | initialAmount | initialSold          | paidAmount | expectedAmount         || expectedSold
        "SELL: Receiving into existing holding should INCREASE amount"      | false | false        | 1000          | new BigDecimal("50") | 200        | new BigDecimal("800")  || new BigDecimal("250")
        "SELL: Receiving into a new holding should INCREASE amount"         | false | true         | 0             | BigDecimal.ZERO      | 200        | new BigDecimal("-200") || new BigDecimal("200")
        "SELL: A null paidAmount should not change the amount"              | false | false        | 1000          | new BigDecimal("50") | null       | new BigDecimal("1000") || new BigDecimal("50")
        "SELL: A null initial 'totalAmountSold' should be handled"          | false | false        | 1000          | null                 | 200        | new BigDecimal("800")  || new BigDecimal("200")
    }

    def "test getOrCreateByPortfolioAndSymbol - existing holding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def symbol = "BTC"
        def existingHolding = Holding.builder()
                .symbol(symbol)
                .portfolio(portfolio)
                .amount(new BigDecimal("1"))
                .amountInUsdt(new BigDecimal("30000"))
                .totalAmountSold(new BigDecimal("0.5"))
                .totalAmountBought(new BigDecimal("1.5"))
                .build()

        when:
        def result = holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol)

        then:
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName(symbol, "Test Portfolio") >> Optional.of(existingHolding)
        0 * holdingRepository.save(_)

        and:
        result == existingHolding
    }

    def "test getOrCreateByPortfolioAndSymbol - new holding"() {
        given:
        def portfolio = new Portfolio(name: "Test Portfolio")
        def symbol = "ETH"

        when:
        def result = holdingService.getOrCreateByPortfolioAndSymbol(portfolio, symbol)

        then:
        1 * holdingRepository.findBySymbolIgnoreCaseAndPortfolioName(symbol, "Test Portfolio") >> Optional.empty()
        0 * holdingRepository.save(_)

        and:
        result.symbol == symbol
        result.portfolio == portfolio
        result.amount == BigDecimal.ZERO
        result.amountInUsdt == BigDecimal.ZERO
        result.totalAmountSold == BigDecimal.ZERO
        result.totalAmountBought == BigDecimal.ZERO
    }
}
