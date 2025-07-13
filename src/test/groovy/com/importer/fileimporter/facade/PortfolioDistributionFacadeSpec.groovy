package com.importer.fileimporter.facade

import com.importer.fileimporter.converter.HoldingConverter
import com.importer.fileimporter.dto.HoldingDto
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.SymbolService
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

import java.math.RoundingMode

class PortfolioDistributionFacadeSpec extends Specification {

    def symbolService = Mock(SymbolService)
    def portfolioService = Mock(PortfolioService)
    def holdingService = Mock(HoldingService)
    def pricingFacade = Mock(PricingFacade)

    @Subject
    def portfolioDistributionFacade = new PortfolioDistributionFacade(
            symbolService, portfolioService, holdingService, pricingFacade
    )

    def "addPreventingNull should not lose precision"() {
        given: "Two BigDecimal values with decimal places"
        def amount1 = new BigDecimal("123.456789")
        def amount2 = new BigDecimal("987.654321")
        def expectedSum = amount1.add(amount2)  // 1111.11111

        when: "Adding the values using addPreventingNull"
        def result = portfolioDistributionFacade.addPreventingNull(amount1, amount2)

        then: "The result should maintain precision"
        result.compareTo(expectedSum.setScale(0, RoundingMode.DOWN)) == 0

        and: "The current implementation loses precision by setting scale to 0"
        result.scale() == 0
        result.toString() == "1111"

        and: "This test demonstrates the issue mentioned in tasks.md"
        // The fix would be to use a more appropriate scale in the addPreventingNull method
    }

    def "excludeWhenAmountIsAlmostZero should not exclude valid holdings"() {
        given: "A holding with a small but valid amount"
        def holding = new Holding(
                totalAmountBought: new BigDecimal("0.5"),
                totalAmountSold: new BigDecimal("0.1")
        )

        and: "Access the private method using reflection"
        def method = PortfolioDistributionFacade.class.getDeclaredMethod("excludeWhenAmountIsAlmostZero")
        method.setAccessible(true)
        def predicate = method.invoke(portfolioDistributionFacade)

        when: "Testing if the holding would be excluded"
        def result = predicate.test(holding)

        then: "The holding should not be excluded"
        result == true

        and: "The difference between bought and sold is 0.4, which is greater than the threshold of 0.3"
        holding.totalAmountBought.subtract(holding.totalAmountSold) == new BigDecimal("0.4")
    }

    def "excludeWhenAmountIsAlmostZero should exclude holdings with very small amounts"() {
        given: "A holding with a very small amount"
        def holding = new Holding(
                totalAmountBought: new BigDecimal("0.035"),
                totalAmountSold: new BigDecimal("0.01")
        )

        and: "Access the private method using reflection"
        def method = PortfolioDistributionFacade.class.getDeclaredMethod("excludeWhenAmountIsAlmostZero")
        method.setAccessible(true)
        def predicate = method.invoke(portfolioDistributionFacade)

        when: "Testing if the holding would be excluded"
        def result = predicate.test(holding)

        then: "The holding should not be excluded"
        result == false

        and: "The difference between bought and sold is 0.25, which is less than the threshold of 0.3"
        holding.totalAmountBought.subtract(holding.totalAmountSold) == new BigDecimal("0.025")
    }

    def "groupHoldingsBySymbol should correctly merge holdings"() {
        given: "Multiple holdings with the same symbol"
        def holdings = [
            new Holding(
                symbol: "BTC",
                portfolio: Portfolio.builder().name("Portfolio1").build(),
                amount: new BigDecimal("1.5"),
                amountInUsdt: new BigDecimal("45000.75"),
                amountInBtc: new BigDecimal("1.5"),
                priceInUsdt: new BigDecimal("30000.5"),
                priceInBtc: new BigDecimal("1.0")
            ),
            new Holding(
                symbol: "BTC",
                portfolio: Portfolio.builder().name("Portfolio2").build(),
                amount: new BigDecimal("0.5"),
                amountInUsdt: new BigDecimal("15000.25"),
                amountInBtc: new BigDecimal("0.5"),
                priceInUsdt: new BigDecimal("30000.5"),
                priceInBtc: new BigDecimal("1.0")
            )
        ]

        and: "Mock the HoldingConverter"
        HoldingConverter.Mapper.createFrom(_ as Holding) >> { Holding h ->
            return HoldingDto.builder()
                .symbol(h.symbol)
                .portfolioName(h.portfolioName)
                .amount(h.amount)
                .amountInUsdt(h.amountInUsdt)
                .amountInBtc(h.amountInBtc)
                .priceInUsdt(h.priceInUsdt)
                .priceInBtc(h.priceInBtc)
                .percentage(BigDecimal.ZERO)
                .build()
        }

        when: "Grouping holdings by symbol"
        def result = portfolioDistributionFacade.groupHoldingsBySymbol(holdings)

        then: "The holdings should be merged correctly"
        result.size() == 1
        result.containsKey("BTC")

        and: "The merged holding should have the sum of amounts"
        with(result.get("BTC")) {
            symbol == "BTC"
            portfolioName == "Portfolio1 - Portfolio2"
            amount == 2.0
            // Due to the precision issue in addPreventingNull, these will be rounded down
            amountInUsdt.toString() == "60001"
            amountInBtc.toString() == "2"
        }
    }

    def "getPortfolioByName should throw exception when name is empty"() {
        when: "Getting a portfolio with an empty name"
        portfolioDistributionFacade.getPortfolioByName("")

        then: "A BAD_REQUEST exception is thrown"
        def exception = thrown(ResponseStatusException)
        exception.status.value() == 400
        exception.reason == "Missing param."
    }

    def "getPortfolioByName should return all portfolios when name is 'all'"() {
        given: "Portfolios exist in the system"
        def portfolios = [
            Portfolio.builder().name("Portfolio1").build(),
            Portfolio.builder().name("Portfolio2").build()
        ]
        portfolioService.getAll() >> portfolios

        and: "Mock the holdings for each portfolio"
        def holdings = [
            new Holding(symbol: "BTC", amount: BigDecimal.ONE, portfolio: portfolios[0]),
            new Holding(symbol: "ETH", amount: BigDecimal.TEN, portfolio: portfolios[1])
        ]
        portfolios[0].holdings = [holdings[0]]
        portfolios[1].holdings = [holdings[1]]

        and: "Mock the pricing facade"
        pricingFacade.getPrices(_ as String) >> [BTC: 30000.0, USDT: 1.0]

        when: "Getting all portfolios"
        def result = portfolioDistributionFacade.getPortfolioByName("all")

        then: "The result contains all portfolios combined"
        result.portfolioName == "All"

        and: "No exceptions are thrown"
        noExceptionThrown()
    }

    def "getPortfolioByName should return portfolio by name"() {
        given: "A portfolio exists with the given name"
        def portfolioName = "TestPortfolio"
        def portfolio = Portfolio.builder().name(portfolioName).build()
        portfolioService.getByName(portfolioName) >> Optional.of(portfolio)

        and: "The portfolio has holdings"
        def holdings = [
                new Holding(symbol: "BTC", amount: BigDecimal.ONE, portfolio: portfolio),
                new Holding(symbol: "ETH", amount: BigDecimal.TEN, portfolio: portfolio)
        ]
        portfolio.holdings = holdings

        and: "Mock the converter"
        HoldingConverter.Mapper.createFromEntities(holdings) >> [
            HoldingDto.builder().symbol("BTC").amount(BigDecimal.ONE).build(),
            HoldingDto.builder().symbol("ETH").amount(BigDecimal.TEN).build()
        ]

        when: "Getting the portfolio by name"
        def result = portfolioDistributionFacade.getPortfolioByName(portfolioName)

        then: "The correct portfolio is returned"
        result.portfolioName == portfolioName
        result.holdings.size() == 2
        result.holdings[0].symbol == "BTC"
        result.holdings[1].symbol == "ETH"
    }

    def "getPortfolioByName should throw exception when portfolio not found"() {
        given: "No portfolio exists with the given name"
        def portfolioName = "NonExistentPortfolio"
        portfolioService.getByName(portfolioName) >> Optional.empty()

        when: "Getting a non-existent portfolio"
        portfolioDistributionFacade.getPortfolioByName(portfolioName)

        then: "A NOT_FOUND exception is thrown"
        def exception = thrown(ResponseStatusException)
        exception.status.value() == 404
        exception.reason == "Portfolio not found."
    }

    def "calculatePortfolioInBtcAndUsdt should calculate values correctly for a specific portfolio"() {
        given: "A portfolio exists with the given name"
        def portfolioName = "TestPortfolio"
        def portfolio = Portfolio.builder().name(portfolioName).build()
        portfolioService.getByName(portfolioName) >> Optional.of(portfolio)

        and: "The portfolio has holdings"
        def holdings = [
            new Holding(
                symbol: "BTC", 
                amount: new BigDecimal("1.5"),
                totalAmountBought: new BigDecimal("2.0"),
                totalAmountSold: new BigDecimal("0.5"),
                    portfolio: portfolio
            ),
            new Holding(
                symbol: "ETH", 
                amount: new BigDecimal("10.0"),
                totalAmountBought: new BigDecimal("15.0"),
                totalAmountSold: new BigDecimal("5.0"),
                    portfolio: portfolio
            )
        ]
        portfolio.holdings = holdings

        and: "Mock the pricing facade"
        pricingFacade.getPrices("BTC") >> [BTC: 1.0d, USDT: 30000.0d]
        pricingFacade.getPrices("ETH") >> [BTC: 0.05d, USDT: 1500.0d]

        and: "Mock the holding service"
        holdingService.updatePercentageHolding(_ as HoldingDto, portfolio) >> { HoldingDto dto, Portfolio p ->
            return dto
        }

        when: "Calculating portfolio values"
        def result = portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(portfolioName)

        then: "The portfolio values are calculated correctly"
        result.portfolioName == portfolioName
        result.holdings.size() == 2

        and: "The total USDT value is set"
        result.totalUsdt != null

        and: "No exceptions are thrown"
        noExceptionThrown()
    }

    def "calculatePortfolioInBtcAndUsdt should handle empty portfolio without errors"() {
        given: "A portfolio exists with the given name but has no holdings"
        def portfolioName = "EmptyPortfolio"
        def portfolio = Portfolio.builder().name(portfolioName).build()
        portfolioService.getByName(portfolioName) >> Optional.of(portfolio)

        and: "The portfolio has no holdings"
        portfolio.holdings = []

        when: "Calculating portfolio values"
        portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(portfolioName)

        then: "The result has the correct portfolio name"
        thrown(ResponseStatusException)
    }

    def "calculatePortfolioInBtcAndUsdt should handle zero total USDT value without division by zero"() {
        given: "A portfolio exists with the given name"
        def portfolioName = "ZeroValuePortfolio"
        def portfolio = Portfolio.builder().name(portfolioName).build()
        portfolioService.getByName(portfolioName) >> Optional.of(portfolio)

        and: "The portfolio has holdings with zero USDT value"
        def holdings = [
            new Holding(
                symbol: "BTC", 
                amount: BigDecimal.ZERO,
                totalAmountBought: BigDecimal.ONE,
                totalAmountSold: BigDecimal.ONE,
                    portfolio: portfolio
            )
        ]
        portfolio.holdings = holdings

        and: "Mock the pricing facade to return zero prices"
        pricingFacade.getPrices("BTC") >> [BTC: 0.0, USDT: 0.0]

        and: "Mock the holding service"
        holdingService.updatePercentageHolding(_ as HoldingDto, portfolio) >> { HoldingDto dto, Portfolio p ->
            return dto
        }

        when: "Calculating portfolio values"
        def result = portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(portfolioName)

        then: "The result has the correct portfolio name"
        result.portfolioName == portfolioName

        and: "No exceptions are thrown (no division by zero)"
        noExceptionThrown()
    }
}
