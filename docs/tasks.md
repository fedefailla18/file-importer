# Portfolio Calculation Improvement Tasks

## Overview
This document contains a detailed list of actionable improvement tasks for the file-importer application, focusing on the calculation of portfolio values from transactions. Each task is presented as a checklist item that can be marked as completed when addressed.

## Transaction Processing

[x] Improve transaction processing logic in TransactionFacade to handle edge cases (Migrated to TransactionProcessor)
[x] Fix potential division by zero in proportion calculation when total amount is zero
[x] Add validation for transaction data before processing
[ ] Implement better error handling for failed transactions
[ ] Add logging for transaction processing steps to aid debugging
[ ] Ensure transaction dates are properly handled across time zones
[ ] Implement the missing functionality in getAmount method when symbols list is empty

## Portfolio Calculation

[x] Review and fix the portfolio calculation logic in buildPortfolio method
[x] Ensure correct aggregation of buy and sell transactions
[x] Fix the commented-out stableTotalCost calculation in HoldingService
[ ] Implement proper handling of fees in portfolio calculations
[x] Add support for calculating realized and unrealized gains/losses
[ ] Ensure portfolio calculations are consistent across different views
[ ] Review the threshold in excludeWhenAmountIsAlmostZero method to prevent excluding valid holdings

## Price History and Retrieval

[x] Improve error handling in PricingFacade to avoid returning empty maps
[x] Implement caching for frequently accessed price data
[ ] Extend the fallback time window for historical price lookups
[ ] Add retry mechanism for external price API calls
[ ] Implement fallback data sources for when primary price source is unavailable
[ ] Add validation for retrieved price data

## Data Consistency

[ ] Ensure consistent decimal precision across all calculations
[ ] Implement transaction reconciliation to detect and fix inconsistencies
[ ] Add data integrity checks for portfolio holdings
[ ] Ensure proper handling of different currency pairs in transactions
[ ] Implement validation for portfolio totals against sum of holdings
[ ] Fix precision loss in addPreventingNull method by using appropriate scale instead of 0
[ ] Implement the TODO in PortfolioDistributionFacade to use converter for creating HoldingDto objects
[ ] Improve the groupHoldingsBySymbol method to handle merging holdings more accurately

## Testing

[ ] Fix and enable the ignored tests in TransactionFacadeSpec
[ ] Add comprehensive unit tests for portfolio calculation logic
[ ] Add integration tests for end-to-end portfolio calculation
[ ] Implement test data generators for various transaction scenarios
[ ] Add tests for edge cases (zero amounts, very small amounts, etc.)
[ ] Implement performance tests for large transaction volumes

## User Experience

[ ] Improve error messages for calculation failures
[ ] Add detailed transaction history view with calculation breakdown
[ ] Implement portfolio performance metrics and visualizations
[ ] Add export functionality for portfolio data
[ ] Implement alerts for significant portfolio changes

## Architecture Improvements

[x] Refactor TransactionFacade to separate concerns (calculation, data retrieval, etc.)
[x] Consider implementing a dedicated portfolio calculation service (Created TransactionProcessor)
[ ] Implement event-driven updates for portfolio values
[ ] Add support for different calculation strategies (FIFO, LIFO, etc.)
[ ] Consider implementing a batch processing system for large transaction volumes

## Documentation

[ ] Document the portfolio calculation algorithm
[ ] Add code comments explaining complex calculation logic
[ ] Create user documentation for understanding portfolio values
[ ] Document known limitations and edge cases
[ ] Create developer guidelines for modifying calculation logic
