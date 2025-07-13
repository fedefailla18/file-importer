package com.importer.fileimporter.utils

class IntegrationTestHelper {

    static List<Map<String, String>> readCsvFile() {
        List<Map<String, String>> rows = []

        File file = getFile()
        println "Reading file from path: ${file.getAbsolutePath()}"

        file.eachLine { line, lineNumber ->
            if (lineNumber == 1) return // Skip the header line
            def fields = line.split(',')
            rows << [
                    'Date(UTC)': fields[0],
                    'Pair'     : fields[1],
                    'Side'     : fields[2],
                    'Price'    : fields[3],
                    'Executed' : fields[4],
                    'Amount'   : fields[5],
                    'Fee'      : fields[6]
            ]
        }
        return rows
    }

    static def getFile() {
        URL resourceUrl = IntegrationTestHelper.class.getResource('/integrationTest/sample_transactions.csv')

        if (resourceUrl == null) {
            println "Resource URL is null. File not found!"
            throw new IllegalStateException("File not found: /integrationTest/sample_transactions.csv")
        }

        File file = new File(resourceUrl.toURI())
        file
    }
}
