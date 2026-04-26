package com.importer.fileimporter.service

import com.importer.fileimporter.dto.BinanceTransactionAdapter
import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.dto.TransactionData
import com.importer.fileimporter.entity.Portfolio
import spock.lang.Specification

class ProcessFileV2Spec extends Specification {

    PortfolioService portfolioService = Mock()
    TransactionProcessor transactionProcessor = Mock()
    FileImporterService fileImporterService = Mock()
    TransactionAdapterFactory transactionAdapterFactory = Mock()
    CoinInformationService coinInformationService = Mock()
    TransactionService transactionService = Mock()

    ProcessFileV2 processFileV2
    
    def setup() {
        processFileV2 = new ProcessFileV2(
                                    portfolioService,
                                    fileImporterService,
                                    transactionAdapterFactory,
                                    transactionProcessor,
                                    coinInformationService,
                                    transactionService
                                )
    }
    
    def "processFile should process rows and return FileInformationResponse"() {
        given: "a multipart file and portfolio name"
        def file = Mock(org.springframework.web.multipart.MultipartFile)
        def portfolioName = "test-portfolio"
        def portfolio = new Portfolio(name: portfolioName)
        def rows = [
            [Executed: "100FET", Price: "1.5", Amount: "150USDT", Fee: "0.1FET", "Date(UTC)": "2023-01-01 10:00:00", Pair: "FETUSDT", Side: "BUY"],
            [Executed: "50SOL", Price: "100", Amount: "5000USDT", Fee: "0.05SOL", "Date(UTC)": "2023-01-02 10:00:00", Pair: "SOLUSDT", Side: "BUY"]
        ]
        
        and: "the file importer service returns rows"
        fileImporterService.getRows(file) >> rows
        
        and: "the portfolio service returns the portfolio"
        portfolioService.findOrSave(portfolioName) >> portfolio
        
        and: "the transaction adapter factory returns transaction data"
        transactionAdapterFactory.createAdapter(rows[0], "BINANCE") >> Mock(TransactionData) {
            getSymbol() >> "FET"
            getSide() >> "BUY"
            getExecuted() >> BigDecimal.valueOf(100)
            getAmount() >> BigDecimal.valueOf(150)
            getDate() >> "2023-01-01 10:00:00"
            getPair() >> "FETUSDT"
            getPrice() >> BigDecimal.valueOf(1.5)
        }
        transactionAdapterFactory.createAdapter(rows[1], "BINANCE") >> Mock(TransactionData) {
            getSymbol() >> "SOL"
            getSide() >> "BUY"
            getExecuted() >> BigDecimal.valueOf(50)
            getAmount() >> BigDecimal.valueOf(5000)
            getDate() >> "2023-01-02 10:00:00"
            getPair() >> "SOLUSDT"
            getPrice() >> BigDecimal.valueOf(100)
        }

        and: "the coin information service returns response for processed symbols"
        coinInformationService.getCoinInformationResponse("FET", _) >> CoinInformationResponse.builder().coinName("FET").build()
        coinInformationService.getCoinInformationResponse("SOL", _) >> CoinInformationResponse.builder().coinName("SOL").build()

        when: "processFile is called"
        def result = processFileV2.processFile(file, null, portfolioName, "BINANCE")
        
        then: "the result should contain processed information"
        result.portfolio == portfolioName
        result.amount == 2
        result.coinInformationResponse.size() == 2
        
        and: "the transaction processor should be called for each row"
        2 * transactionProcessor.process(_)
    }
    
    def "getAdapter should use the transaction adapter factory to create an adapter"() {
        given: "a row and a file type"
        def row = [Executed: "100FET", Price: "1.5", Amount: "150USDT", Fee: "0.1FET"]
        def fileType = "MEXC"
        def mockAdapter = Mock(TransactionData)
        
        and: "the transaction adapter factory returns a mock adapter"
        transactionAdapterFactory.createAdapter(row, fileType) >> mockAdapter
        
        when: "getAdapter is called"
        def result = processFileV2.getAdapter(row, fileType)
        
        then: "the result should be the mock adapter"
        result == mockAdapter
    }
    
    def "getAdapter should use 'Binance' as default file type if null is provided"() {
        given: "a row and a null file type"
        def row = [Executed: "100FET", Price: "1.5", Amount: "150USDT", Fee: "0.1FET"]
        def mockAdapter = Mock(BinanceTransactionAdapter)
        
        when: "getAdapter is called with null file type"
        processFileV2.getAdapter(row, null)
        
        then: "the transaction adapter factory returns a mock adapter for 'Binance'"
        1 * transactionAdapterFactory.createAdapter(row, "Binance")>> mockAdapter
    }
    
}