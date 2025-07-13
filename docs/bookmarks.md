# File Importer Project Bookmarks

This document contains bookmarks for important areas of the codebase that may need attention in future development.

## CalculateAmountSpent

The `CalculateAmountSpent` class is responsible for calculating the amount spent in USDT for transactions. It's used in the `CoinInformationService` to track spending and profits.

- [CalculateAmountSpent.java](/src/main/java/com/importer/fileimporter/service/usecase/CalculateAmountSpent.java)
- [CalculateAmountSpentSpec.groovy](/src/test/groovy/com/importer/fileimporter/service/usecase/CalculateAmountSpentSpec.groovy)

### Future Improvements

1. **Realized Profit Calculation**: The current implementation tracks total realized profit but doesn't calculate profit per transaction based on cost basis. Consider implementing a more sophisticated profit calculation that takes into account the average cost of the coins being sold.

2. **Cost Basis Tracking**: Add functionality to track the cost basis of holdings over time, which would enable more accurate profit calculations.

3. **Transaction Fee Handling**: Currently, transaction fees are not factored into profit calculations. Consider adding support for this.

## HoldingService

The `HoldingService` class manages holdings for portfolios. It includes functionality to update holdings based on transactions.

- [HoldingService.java](/src/main/java/com/importer/fileimporter/service/HoldingService.java)
- [HoldingServiceSpec.groovy](/src/test/groovy/com/importer/fileimporter/service/HoldingServiceSpec.groovy)

### Future Improvements

1. **StableTotalCost Tracking**: The code previously had commented-out sections related to tracking stableTotalCost. Consider implementing this functionality if it would be useful.

2. **Performance Optimization**: The current implementation makes multiple database calls when updating holdings. Consider optimizing this to reduce database load.

## CoinInformationService

The `CoinInformationService` class processes transactions to generate information about coin holdings.

- [CoinInformationService.java](/src/main/java/com/importer/fileimporter/service/CoinInformationService.java)
- [CoinInformationServiceSpec.groovy](/src/test/groovy/com/importer/fileimporter/service/CoinInformationServiceSpec.groovy)

### Future Improvements

1. **Transaction Processing**: The current implementation marks transactions as processed after processing them. Consider adding functionality to reprocess transactions if needed.

2. **Error Handling**: Add more robust error handling for cases where pricing information is unavailable or transactions are invalid.

## Testing

The project has good test coverage, but there are some areas that could be improved:

1. **Integration Tests**: Add more integration tests that verify the entire flow from transaction creation to holding updates.

2. **Edge Cases**: Add tests for edge cases like transactions with zero amounts, negative prices, etc.

3. **Performance Tests**: Add tests to verify the performance of the system with large numbers of transactions.

## General Improvements

1. **Documentation**: Add more comprehensive documentation for the project, including architecture diagrams and usage examples.

2. **Logging**: Enhance logging throughout the application to make debugging easier.

3. **Error Handling**: Implement a consistent error handling strategy across the application.

4. **API Documentation**: Add Swagger or similar API documentation for the REST endpoints.