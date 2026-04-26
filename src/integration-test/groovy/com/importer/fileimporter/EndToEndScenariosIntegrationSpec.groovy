package com.importer.fileimporter

import com.importer.fileimporter.dto.TransactionDto
import com.importer.fileimporter.dto.integration.CryptoCompareResponse
import com.importer.fileimporter.facade.PortfolioDistributionFacade
import com.importer.fileimporter.service.CryptoCompareProxy
import com.importer.fileimporter.service.ProcessFileFactory
import com.importer.fileimporter.service.TransactionFacade
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.utils.IntegrationTestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Stepwise
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when

@Stepwise
@Transactional
@Rollback(false)
class EndToEndScenariosIntegrationSpec extends BaseIntegrationSpec {

    def setup() {
        // Setup mock prices
        // BTC price: 50000 USDT, 1.0 BTC (identity)
        // ETH price: 2500 USDT, 0.05 BTC
        def btcPrices = ["USDT": 50000.0, "BTC": 1.0]
        def ethPrices = ["USDT": 2500.0, "BTC": 0.05]
        def allPrices = [
                "BTC": btcPrices,
                "ETH": ethPrices,
                "FET": ["USDT": 1.0, "BTC": 0.00002],
                "WAVES": ["USDT": 2.0, "BTC": 0.00004],
                "BAND": ["USDT": 1.5, "BTC": 0.00003],
                "IMX": ["USDT": 2.0, "BTC": 0.00004],
                "TIA": ["USDT": 10.0, "BTC": 0.0002],
                "AEVO": ["USDT": 1.0, "BTC": 0.00002],
                "API3": ["USDT": 2.5, "BTC": 0.00005],
                "XRP": ["USDT": 0.6, "BTC": 0.00001],
                "ADA": ["USDT": 0.5, "BTC": 0.00001],
                "NEAR": ["USDT": 5.0, "BTC": 0.0001],
                "SOL": ["USDT": 150.0, "BTC": 0.003],
                "FTM": ["USDT": 0.8, "BTC": 0.000016],
                "1INCH": ["USDT": 0.4, "BTC": 0.000008]
        ]

        when(cryptoCompareProxy.getHistoricalData(eq("BTC"), eq("USDT"), anyLong())).thenReturn(createMockCryptoCompareResponse(50000.0))
        when(cryptoCompareProxy.getHistoricalData(eq("ETH"), eq("USDT"), anyLong())).thenReturn(createMockCryptoCompareResponse(2500.0))
        when(cryptoCompareProxy.getHistoricalData(eq("ETH"), eq("BTC"), anyLong())).thenReturn(createMockCryptoCompareResponse(0.05))

        when(cryptoCompareProxy.getData(anyString(), anyString())).thenAnswer({ invocation ->
            String symbol = invocation.getArgument(0)
            return allPrices.get(symbol) ?: [:]
        })
        
        when(cryptoCompareProxy.getData(anyList(), anyString())).thenReturn(allPrices)
    }

    private static CryptoCompareResponse createMockCryptoCompareResponse(double price) {
        def response = new CryptoCompareResponse()
        response.response = "Success"
        def data = new CryptoCompareResponse.Data()
        def chartData = new CryptoCompareResponse.ChartData()
        chartData.high = BigDecimal.valueOf(price)
        chartData.time = LocalDateTime.now() 
        data.chartDataList = [chartData]
        response.data = data
        return response
    }

    @Rollback(false)
    def "Scenario 1 & 2: Uploading BTC transactions and selling part for profit"() {
        given: "A portfolio name"
        def portfolioName = "InvestorA"

        when: "I add a BUY transaction for 1.0 BTC at 40000 USDT"
        transactionFacade.save(TransactionDto.builder()
                .symbol("BTC")
                .side("BUY")
                .executed(BigDecimal.valueOf(1.0))
                .price(BigDecimal.valueOf(40000))
                .pair("BTCUSDT")
                .paidWith("USDT")
                .paidAmount(BigDecimal.valueOf(40000))
                .portfolioName(portfolioName)
                .dateUtc(LocalDateTime.now().minusDays(2))
                .createdBy(defaultUser.username)
                .build())
        
        transactionService.flush()

        then: "Holding for BTC should be 1.0"
        def holdings = holdingService.getBySymbol("BTC")
        def holding = holdings.find { it.portfolioName == portfolioName }
        holding != null
        holding.amount == 1.0
        holding.stableTotalCost == 40000.0

        when: "I sell 0.5 BTC at 60000 USDT"
        transactionFacade.save(TransactionDto.builder()
                .symbol("BTC")
                .side("SELL")
                .executed(BigDecimal.valueOf(0.5))
                .price(BigDecimal.valueOf(60000))
                .pair("BTCUSDT")
                .paidWith("USDT")
                .paidAmount(BigDecimal.valueOf(30000))
                .portfolioName(portfolioName)
                .dateUtc(LocalDateTime.now().minusDays(1))
                .createdBy(defaultUser.username)
                .build())

        transactionService.flush()

        then: "Holding for BTC should be 0.5 and realized profit 10000"
        def updatedHoldings = holdingService.getBySymbol("BTC")
        def updatedHolding = updatedHoldings.find { it.portfolioName == portfolioName }
        updatedHolding.amount == 0.5
        // Cost basis was 40000 for 1.0 BTC -> 20000 for 0.5 BTC.
        // Sale price 30000 - Cost 20000 = 10000 profit.
        updatedHolding.totalRealizedProfitUsdt == 10000.0
        updatedHolding.stableTotalCost == 20000.0
    }

    @Rollback(false)
    def "Scenario 3: Non-stable trade BTC to ETH"() {
        given: "Portfolio 'InvestorA' with 0.5 BTC"
        def portfolioName = "InvestorA"
        
        when: "I trade 0.1 BTC for 2 ETH"
        // Buy 2 ETH, paying with 0.1 BTC. Price 2500 USDT/ETH (but we use ETHBTC pair)
        transactionFacade.save(TransactionDto.builder()
                .symbol("ETH")
                .side("BUY")
                .executed(BigDecimal.valueOf(2.0))
                .price(BigDecimal.valueOf(0.05))
                .pair("ETHBTC")
                .paidWith("BTC")
                .paidAmount(BigDecimal.valueOf(0.1))
                .portfolioName(portfolioName)
                .dateUtc(LocalDateTime.now())
                .createdBy(defaultUser.username)
                .build())

        transactionService.flush()

        then: "BTC holding should decrease and ETH holding should increase"
        def btcHoldings = holdingService.getBySymbol("BTC")
        def btcHolding = btcHoldings.find { it.portfolioName == portfolioName }
        def ethHoldings = holdingService.getBySymbol("ETH")
        def ethHolding = ethHoldings.find { it.portfolioName == portfolioName }
        
        btcHolding.amount == 0.4 // 0.5 - 0.1
        ethHolding.amount == 2.0
        
        and: "Realized profit for BTC should be updated because we 'sold' 0.1 BTC"
        btcHolding.totalRealizedProfitUsdt == 11000.0
    }

    @Rollback(false)
    def "Scenario 4: Portfolio Valuation and Distribution"() {
        given: "Portfolio 'InvestorA' with 0.4 BTC and 2.0 ETH"
        def portfolioName = "InvestorA"
        
        when: "Calculating portfolio distribution"
        // Ensure symbols have prices for valuation by using the calculate method
        def distribution = portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(portfolioName)
        
        then: "Distribution should show correct totals in USDT and BTC"
        // 0.4 BTC * 50000 + 2.0 ETH * 2500 = 20000 + 5000 = 25000 USDT
        distribution.totalUsdt != null
        distribution.totalUsdt == 25000.0
        distribution.totalInBTC == 25000.0 / 50000.0
        
        and: "Holdings list should be correct"
        distribution.holdings.size() == 2
        distribution.holdings.any { it.symbol == "BTC" && it.amount == 0.4 }
        distribution.holdings.any { it.symbol == "ETH" && it.amount == 2.0 }
    }

    @Rollback(false)
    def "Multi-portfolio isolation: 'InvestorB' with same bases"() {
        given: "A second portfolio 'InvestorB'"
        def portfolioName = "InvestorB"

        when: "InvestorB buys 1.0 BTC at 30000 USDT"
        transactionFacade.save(TransactionDto.builder()
                .symbol("BTC")
                .side("BUY")
                .executed(BigDecimal.valueOf(1.0))
                .price(BigDecimal.valueOf(30000))
                .pair("BTCUSDT")
                .paidWith("USDT")
                .paidAmount(BigDecimal.valueOf(30000))
                .portfolioName(portfolioName)
                .dateUtc(LocalDateTime.now())
                .createdBy(defaultUser.username)
                .build())

        then: "InvestorB holding is separate from InvestorA"
        def holdingB = holdingService.getBySymbol("BTC").find { it.portfolioName == portfolioName }
        def holdingA = holdingService.getBySymbol("BTC").find { it.portfolioName == "InvestorA" }
        
        holdingB.amount == 1.0
        holdingB.stableTotalCost == 30000.0
        
        holdingA.amount == 0.4
    }

    @Rollback(false)
    def "End-to-end: Uploading Binance CSV"() {
        given: "A new portfolio 'BinanceUpload'"
        def portfolioName = "BinanceUpload"
        def file = new MockMultipartFile("test.csv", "sample_transactions.csv", "text/csv", 
                new FileInputStream(IntegrationTestHelper.getFile()))

        when: "I upload the Binance sample file"
        def response = processFileFactory.processFile(file, null, portfolioName, "Binance")

        then: "Transactions and holdings are created"
        response.portfolio == portfolioName
        response.amount > 0
        
        then: "End-to-end results are consistent"
        // Calculate distribution first to ensure prices are fetched and holdings updated
        def distribution = portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(portfolioName)
        println "[DEBUG_LOG] distribution holdings size: ${distribution.holdings.size()}"
        distribution.portfolioName == portfolioName
        distribution.holdings.size() > 0
    }
}
