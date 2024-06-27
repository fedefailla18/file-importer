package com.importer.fileimporter.service

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.dto.FileInformationResponse
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Subject

import static com.importer.fileimporter.utils.TestHelper.readCsvFile

class ProcessFileSpec extends Specification {

    List<String> allSymbols = 'SOL,WAVES,AEVO,FET,IMX,XRP,ETH,BAND,NEAR,TIA,FTM,ADA'.split(',')

    @Subject
    ProcessFile processFile

    FileImporterService fileImporterService = Mock()
    TransactionService transactionService = Mock()
    SymbolService symbolService = Mock()

    def setup() {
        processFile = new ProcessFile(fileImporterService, transactionService, symbolService)
    }

    def "should process file and return file information response"() {
        given: "a multipart file with transaction data and predefined symbols"
        MultipartFile file = Mock(MultipartFile)
        List<Map<String, String>> mockRows = readCsvFile()
        List<String> symbols = OperationUtils.SYMBOL // this is not used anymore

        and: "file importer service returns rows"
        fileImporterService.getRows(file) >> mockRows

        and: "symbol service returns default symbols if symbols are not provided"
        symbolService.getAllSymbols() >> symbols

        when: "processFile is called"
        FileInformationResponse response = processFile.processFile(file)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == allSymbols.size()
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }
    }

    def "should process file with custom symbols and return file information response"() {
        given: "a multipart file with transaction data and custom symbols"
        MultipartFile file = Mock(MultipartFile)
        List<Map<String, String>> mockRows = readCsvFile()
        List<String> customSymbols = ["FET", "WAVES"]

        and: "file importer service returns rows"
        fileImporterService.getRows(file) >> mockRows

        when: "processFile is called with custom symbols"
        FileInformationResponse response = processFile.processFile(file, customSymbols)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == allSymbols.size()
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }
    }
    def "test transaction calculations"() {
        given: "Mock data for transactions"
        MultipartFile file = Mock(MultipartFile)
        List<String> symbols = ['BAND', 'BTC', 'USDT']
        List<Map<String, String>> rows = [
                [
                        "Date(UTC)": "2024-06-17 22:32:10",
                        "Pair"    : "BANDBTC",
                        "Side"    : "SELL",
                        "Price"   : "1.8810000000",
                        "Executed": "30.0000000000BAND",
                        "Amount"  : "56.43000000USDT",
                        "Fee"     : "0.0000000000USDT"
                ],
                [
                        "Date(UTC)": "2024-06-03 16:46:25",
                        "Pair"    : "BANDBTC",
                        "Side"    : "SELL",
                        "Price"   : "1.6810000000",
                        "Executed": "90.0000000000BAND",
                        "Amount"  : "151.20000000USDT",
                        "Fee"     : "0.1512900000USDT"
                ],
                [
                        "Date(UTC)": "2024-05-31 21:00:00",
                        "Pair"    : "BANDBTC",
                        "Side"    : "BUY",
                        "Price"   : "1.0270000000",
                        "Executed": "170.0000000000BAND",
                        "Amount"  : "225.59000000USDT",
                        "Fee"     : "1.0000000000BAND"
                ]
        ]

        fileImporterService.getRows(file) >> rows
        symbolService.getAllSymbols() >> symbols

        when: "Processing the file"
        FileInformationResponse response = processFile.processFile(file)

        then: "The amounts are calculated correctly"
        response.amount == 3

        and: "Transaction details for BAND are correct"
        def bandInfo = response.coinInformationResponse.find { it.coinName == 'BAND' }
        bandInfo.amount == new BigDecimal("50.0000000000")
//        bandInfo.usdSpent == new BigDecimal("17.9751290000") Commenting since it's not used for now
    }

    def "calculateSpent test"() {
        given: "A CoinInformationResponse object and an amount to add/subtract"
        def coinInfo = CoinInformationResponse.createEmpty("BAND")
        coinInfo.spent.put("USDT", new BigDecimal("10"))

        when: "Calculating spent amount for buy and sell operations"
        processFile.calculateSpent(new BigDecimal("5"), coinInfo, "USDT", true)
        processFile.calculateSpent(new BigDecimal("2"), coinInfo, "USDT", false)

        then: "Spent amounts are updated correctly"
        coinInfo.spent["USDT"] == new BigDecimal("13")
    }

    def "calculateAmount test"() {
        given: "An initial amount and an amount to add/subtract"
        def initialAmount = new BigDecimal("10")
        def amountToAdd = new BigDecimal("5")
        def amountToSubtract = new BigDecimal("3")

        expect: "Amounts are updated correctly"
        processFile.calculateAmount(initialAmount, true, amountToAdd) == new BigDecimal("15")
        processFile.calculateAmount(initialAmount, false, amountToSubtract) == new BigDecimal("7")
    }
}
