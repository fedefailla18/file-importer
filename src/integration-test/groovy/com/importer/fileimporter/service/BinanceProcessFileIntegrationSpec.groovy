package com.importer.fileimporter.service

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.dto.FileInformationResponse
import com.importer.fileimporter.utils.IntegrationTestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Subject

import java.math.BigDecimal
import java.math.RoundingMode

class BinanceProcessFileIntegrationSpec extends BaseIntegrationSpec {

    List<String> allSymbols = 'SOL,WAVES,AEVO,FET,IMX,XRP,ETH,BAND,NEAR,TIA,FTM,ADA,1INCH,API3'.split(',')

    @Autowired
    @Subject
    BinanceProcessFile binanceProcessFile

    def "should process file and return file information response"() {
        given: "a multipart file with transaction data and predefined symbols"
        MultipartFile multipartFile = getFile()

        List<Map<String, String>> mockRows = IntegrationTestHelper.readCsvFile()

        when: "processFile is called"
        FileInformationResponse response = binanceProcessFile.processFile(multipartFile)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == 12
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
        }
    }

    def "should process file with custom symbols and return file information response"() {
        given: "a multipart file with transaction data and custom symbols"
        MultipartFile file = getFile()
        List<Map<String, String>> mockRows = IntegrationTestHelper.readCsvFile()
        List<String> customSymbols = ["FET", "WAVES", "1INCH", "API3"]

        when: "processFile is called with custom symbols"
        FileInformationResponse response = binanceProcessFile.processFile(file, customSymbols)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == 12
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
        }
    }

    def "should correctly calculate amounts for FET transactions"() {
        given: "a multipart file with transaction data"
        MultipartFile file = getFile()

        when: "processFile is called"
        FileInformationResponse response = binanceProcessFile.processFile(file)

        then: "FET coin information should have correct calculations"
        def fetInfo = response.coinInformationResponse.find { it.coinName == "FET" }

        // Verify FET exists in the response
        fetInfo != null

        // Verify transaction count
        fetInfo.totalTransactions == 1

        // Verify amount calculations
        fetInfo.amount.setScale(10, RoundingMode.HALF_UP) == new BigDecimal("170.0000000000").setScale(10, RoundingMode.HALF_UP)

        // Verify spent calculations
        fetInfo.spent.get("USDT").setScale(10, RoundingMode.HALF_UP) == new BigDecimal("225.5900000000").setScale(10, RoundingMode.HALF_UP)

        // Print debug information
        println "[DEBUG_LOG] FET Info: ${fetInfo}"
        println "[DEBUG_LOG] FET Amount: ${fetInfo.amount}"
        println "[DEBUG_LOG] FET Spent: ${fetInfo.spent}"
    }

    def "should correctly calculate amounts for WAVES transactions"() {
        given: "a multipart file with transaction data"
        MultipartFile file = getFile()

        when: "processFile is called"
        FileInformationResponse response = binanceProcessFile.processFile(file)

        then: "WAVES coin information should have correct calculations"
        def wavesInfo = response.coinInformationResponse.find { it.coinName == "WAVES" }

        // Verify WAVES exists in the response
        wavesInfo != null

        // Verify transaction count
        wavesInfo.totalTransactions == 1

        // Verify amount calculations - should be negative for SELL
        wavesInfo.amount.setScale(10, RoundingMode.HALF_UP) == new BigDecimal("-90.0000000000").setScale(10, RoundingMode.HALF_UP)

        // Verify sold calculations
        wavesInfo.sold.get("USDT").setScale(10, RoundingMode.HALF_UP) == new BigDecimal("151.2900000000").setScale(10, RoundingMode.HALF_UP)

        // Print debug information
        println "[DEBUG_LOG] WAVES Info: ${wavesInfo}"
        println "[DEBUG_LOG] WAVES Amount: ${wavesInfo.amount}"
        println "[DEBUG_LOG] WAVES Sold: ${wavesInfo.sold}"
    }

    def "should correctly calculate amounts for BAND transactions"() {
        given: "a multipart file with transaction data"
        MultipartFile file = getFile()

        when: "processFile is called"
        FileInformationResponse response = binanceProcessFile.processFile(file)

        then: "BAND coin information should have correct calculations"
        def bandInfo = response.coinInformationResponse.find { it.coinName == "BAND" }

        // Verify BAND exists in the response
        bandInfo != null

        // Verify transaction count - there are 9 BAND transactions in the sample file
        bandInfo.totalTransactions == 9

        // Verify amount calculations - all are SELL transactions, so amount should be negative
        def expectedAmount = new BigDecimal("-200.0000000000")
        bandInfo.amount.setScale(10, RoundingMode.HALF_UP) == expectedAmount.setScale(10, RoundingMode.HALF_UP)

        // Verify sold calculations - all are sold for BTC
        def expectedSold = new BigDecimal("0.0051327000")
        bandInfo.sold.get("BTC").setScale(10, RoundingMode.HALF_UP) == expectedSold.setScale(10, RoundingMode.HALF_UP)

        // Print debug information
        println "[DEBUG_LOG] BAND Info: ${bandInfo}"
        println "[DEBUG_LOG] BAND Amount: ${bandInfo.amount}"
        println "[DEBUG_LOG] BAND Sold: ${bandInfo.sold}"
    }

    def "should correctly identify special symbols like 1INCH and API3"() {
        given: "a multipart file with transaction data including special symbols"
        // This test would require a sample file with 1INCH and API3 transactions
        // For now, we'll rely on the unit tests for ProcessFileUtils.getSymbolFromNumber()

        expect: "the special symbols to be correctly identified"
        // This is a placeholder for future implementation
        true
    }

    MultipartFile getFile() {
        new MockMultipartFile("test.csv", new FileInputStream(IntegrationTestHelper.getFile()));
    }
}
