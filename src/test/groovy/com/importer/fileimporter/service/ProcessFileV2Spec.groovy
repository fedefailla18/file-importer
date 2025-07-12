package com.importer.fileimporter.service

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.dto.TransactionData
import com.importer.fileimporter.entity.Portfolio
import spock.lang.Specification

class ProcessFileV2Spec extends Specification {

    PortfolioService portfolioService = Mock()
    TransactionService transactionService = Mock()
    FileImporterService fileImporterService = Mock()
    TransactionAdapterFactory transactionAdapterFactory = Mock()
    
    ProcessFileV2 processFileV2
    
    def setup() {
        processFileV2 = new ProcessFileV2(
            portfolioService,
            transactionService,
            fileImporterService,
            transactionAdapterFactory
        )
    }
    
    def "getTransactionDetailInformation should process rows and return transaction details map"() {
        given: "a list of rows and a portfolio name"
        def rows = [
            [Executed: "100FET", Price: "1.5", Amount: "150USDT", Fee: "0.1FET", "Date(UTC)": "2023-01-01", Pair: "FETUSDT", Side: "BUY"],
            [Executed: "50SOL", Price: "100", Amount: "5000USDT", Fee: "0.05SOL", "Date(UTC)": "2023-01-02", Pair: "SOLUSDT", Side: "BUY"]
        ]
        def portfolioName = "test-portfolio"
        def portfolio = new Portfolio(name: portfolioName)
        
        and: "the portfolio service returns the portfolio"
        portfolioService.findOrSave(portfolioName) >> portfolio
        
        and: "the transaction adapter factory returns transaction data for FET"
        transactionAdapterFactory.createAdapter(rows[0], portfolioName) >> Mock(TransactionData) {
            getSymbol() >> "FET"
            getSide() >> "BUY"
            getExecuted() >> BigDecimal.valueOf(100)
            getAmount() >> BigDecimal.valueOf(150)
            getCoinName() >> "FET"
            getPaidWith() >> "USDT"
            getDate() >> "2023-01-01"
            getPair() >> "FETUSDT"
            getPrice() >> BigDecimal.valueOf(1.5)
            getFee() >> BigDecimal.valueOf(0.1)
        }
        
        and: "the transaction adapter factory returns transaction data for SOL"
        transactionAdapterFactory.createAdapter(rows[1], portfolioName) >> Mock(TransactionData) {
            getSymbol() >> "SOL"
            getSide() >> "BUY"
            getExecuted() >> BigDecimal.valueOf(50)
            getAmount() >> BigDecimal.valueOf(5000)
            getCoinName() >> "SOL"
            getPaidWith() >> "USDT"
            getDate() >> "2023-01-02"
            getPair() >> "SOLUSDT"
            getPrice() >> BigDecimal.valueOf(100)
            getFee() >> BigDecimal.valueOf(0.05)
        }
        
        when: "getTransactionDetailInformation is called"
        def result = processFileV2.getTransactionDetailInformation(rows, null, portfolioName)
        
        then: "the result should contain transaction details for each symbol"
        result.size() == 2
        result.containsKey("FET")
        result.containsKey("SOL")
        
        and: "the transaction service should be called for each row"
        2 * transactionService.saveTransaction(_, _, _, _, _, _, _, _, _, _, _)
    }
    
    def "getAdapter should use the transaction adapter factory to create an adapter"() {
        given: "a row and a portfolio name"
        def row = [Executed: "100FET", Price: "1.5", Amount: "150USDT", Fee: "0.1FET"]
        def portfolioName = "test-portfolio"
        def mockAdapter = Mock(TransactionData)
        
        and: "the transaction adapter factory returns a mock adapter"
        transactionAdapterFactory.createAdapter(row, portfolioName) >> mockAdapter
        
        when: "getAdapter is called"
        def result = processFileV2.getAdapter(row, portfolioName)
        
        then: "the result should be the mock adapter"
        result == mockAdapter
    }
    
    def "getAdapter should use 'Binance' as default portfolio name if null is provided"() {
        given: "a row and a null portfolio name"
        def row = [Executed: "100FET", Price: "1.5", Amount: "150USDT", Fee: "0.1FET"]
        def mockAdapter = Mock(TransactionData)
        
        and: "the transaction adapter factory returns a mock adapter for 'Binance'"
        transactionAdapterFactory.createAdapter(row, "Binance") >> mockAdapter
        
        when: "getAdapter is called with null portfolio name"
        def result = processFileV2.getAdapter(row, null)
        
        then: "the result should be the mock adapter and factory should be called with 'Binance'"
        result == mockAdapter
        1 * transactionAdapterFactory.createAdapter(row, "Binance")
    }
    
    def "processTransactionRow should update transaction details and save transaction"() {
        given: "transaction data, symbol, transaction details map, and portfolio"
        def transactionData = Mock(TransactionData) {
            getSide() >> "BUY"
            getSymbol() >> "FET"
            getExecuted() >> BigDecimal.valueOf(100)
            getAmount() >> BigDecimal.valueOf(150)
            getCoinName() >> "FET"
            getPaidWith() >> "USDT"
            getDate() >> "2023-01-01"
            getPair() >> "FETUSDT"
            getPrice() >> BigDecimal.valueOf(1.5)
            getFee() >> BigDecimal.valueOf(0.1)
        }
        def symbol = "FET"
        def transactionDetails = [:]
        def portfolio = new Portfolio(name: "test-portfolio")
        
        when: "processTransactionRow is called"
        processFileV2.processTransactionRow(transactionData, symbol, transactionDetails, portfolio)
        
        then: "transaction details should be updated"
        transactionDetails.containsKey(symbol)
        transactionDetails[symbol] instanceof CoinInformationResponse
        
        and: "transaction should be saved"
        1 * transactionService.saveTransaction(
            "FET", "USDT", "2023-01-01", "FETUSDT", 
            "BUY", BigDecimal.valueOf(1.5), BigDecimal.valueOf(100), 
            BigDecimal.valueOf(150), BigDecimal.valueOf(0.1), 
            _, portfolio
        )
    }
}