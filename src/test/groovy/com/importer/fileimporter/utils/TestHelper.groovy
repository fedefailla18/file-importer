package com.importer.fileimporter.utils

class TestHelper {

    static String filePath = 'src/test/resources/sample_transactions.csv'

    static List<Map<String, String>> readCsvFile() {
        List<Map<String, String>> rows = []
        def file = new File(filePath)
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
}
