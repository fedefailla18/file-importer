# Test Coverage Improvements

This document outlines the areas of the application that require better test coverage.

The overall test coverage of the project is very low:
- **Instruction Coverage:** 36%
- **Branch Coverage:** 9%

To improve the quality and robustness of the application, it is highly recommended to add more tests to the following areas.

## Areas with Low Test Coverage

The following is a list of packages and classes with low or zero test coverage.

### Security (`com.importer.fileimporter.config.security` and `com.importer.fileimporter.controller.security`)

Most of the security-related classes have 0% test coverage. This is a critical area that needs to be tested thoroughly.

**Classes with 0% coverage:**
- `JwtAuthenticationFilter.java`
- `JwtService.java`
- `UserDetailsImpl.java`
- `UserDetailsServiceImpl.java`
- `AuthController.java`

**Missing Tests:**
- **Integration Tests** for `AuthController` to test the authentication and authorization endpoints (e.g., `/api/auth/signin`, `/api/auth/signup`).
- **Unit Tests** for `JwtService` to test the JWT generation and validation logic.
- **Unit Tests** for `UserDetailsServiceImpl` to test the user loading logic.

### Controllers (`com.importer.fileimporter.controller`)

All controllers have 0% test coverage. The API endpoints are not being tested.

**Classes with 0% coverage:**
- `HoldingController.java`
- `PortfolioController.java`
- `PricingController.java`
- `TransactionController.java`
- `WebController.java`

**Missing Tests:**
- **Integration Tests** for all controllers to test the API endpoints. This includes testing the request and response formats, status codes, and the interaction with the services.

### Services (`com.importer.fileimporter.service`)

Many services have low or zero test coverage. The core business logic of the application is not being tested.

**Classes with 0% coverage:**
- `CoinInformationService.java`
- `CryptoCompareProxy.java`
- `FileImporterService.java`
- `HoldingService.java`
- `PortfolioService.java`
- `PriceHistoryService.java`
- `SymbolService.java`
- `TransactionService.java`

**Missing Tests:**
- **Unit Tests** for all services to test the business logic. This includes testing the different scenarios, edge cases, and the interaction with the repositories and other services.

### Facades (`com.importer.fileimporter.facade`)

The facades, which are responsible for coordinating the work between different services, are not being tested.

**Classes with 0% coverage:**
- `CoinInformationFacade.java`
- `PortfolioDistributionFacade.java`
- `PricingFacade.java`

**Missing Tests:**
- **Unit Tests** for all facades to test the coordination logic and the interaction with the services.

## Conclusion

Adding tests to these areas will significantly improve the quality of the application and will help to prevent bugs and regressions in the future.
