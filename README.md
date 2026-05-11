# File-importer
File-importer (InvestTracker) is a specialized Spring Boot application designed for crypto investors to track their portfolio's performance, cost basis, and realized/unrealized gains.

It was born from the need to accurately account for historical transactions, especially through volatile market cycles (like the 2021 bull run and subsequent dips), providing a clear view of how a portfolio appreciates or depreciates over time.

## Core Features

- **Historical Ingestion**: Support for importing large volumes of transaction data from exchanges (Binance, MEXC) via CSV/Excel.
- **Multi-Exchange Sync**: Automatically sync your trading history directly from Binance, MEXC, and IOL. [See Integrations Guide](docs/exchange-integrations-guide.md).
- **Accurate Accounting**: Precise cost basis tracking (Average Cost) and Realized Profit/Loss calculation.
- **Portfolio Valuation**: Real-time (cached) market value tracking in USDT.
- **DIP Analytics**: Track your "buying the dip" efficiency by monitoring your average entry prices.

## Useful resources

To get prices in real time we use CryptoCompare. You will need an API key:

        https://www.cryptocompare.com/cryptopian/api-keys

## Getting Started

### Authentication
InvestTracker requires authentication for most endpoints. See the [Authentication Guide](docs/authentication-guide.md) for instructions on how to register and login.

## API Documentation

The API is documented using OpenAPI 3.0 (Swagger). After starting the application, you can access the documentation at:

        http://localhost:9080/swagger-ui.html

This provides an interactive interface to explore and test all available endpoints.

For information on how to document new APIs, see the [API Documentation Guide](docs/api-documentation-guide.md).

## Running tests

### Unit tests

To run unit tests:

        ./gradlew test

### Integration tests

To run integration tests:

        ./gradlew integrationTest

### Test coverage

JaCoCo is configured to generate test coverage reports. After running tests, you can find the reports at:

- Unit tests coverage: `build/reports/jacoco/test/html/index.html`
- Integration tests coverage: `build/reports/jacoco/integrationTest/index.html`
- Combined coverage: `build/reports/jacoco/allTests/index.html`

To generate all test coverage reports:

        ./gradlew jacocoAllTestReport

For more information on test coverage and how to improve it, see the [Test Coverage Guide](docs/test-coverage-guide.md).
