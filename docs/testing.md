# Testing & Coverage

How to run/read JaCoCo coverage, plus the current, re-verified list of coverage gaps. (Merged 2026-09-12 from `test-coverage-guide.md` + `TEST_COVERAGE_IMPROVEMENTS.md` — the latter's numbers and class list were stale; re-checked against `src/test`/`src/integration-test` directly rather than trusted as-is.)

## Running Coverage Reports

```bash
./gradlew test jacocoTestReport                    # Unit test coverage
./gradlew integrationTest jacocoIntegrationTestReport  # Integration test coverage
./gradlew jacocoAllTestReport                      # Combined
```

Reports:
- Unit: `build/reports/jacoco/test/html/index.html`
- Integration: `build/reports/jacoco/integrationTest/index.html`
- Combined: `build/reports/jacoco/allTests/index.html`

## Coverage Goals

- **Line coverage**: ≥ 80%
- **Branch coverage**: ≥ 70%

## Metrics JaCoCo reports

Instructions (bytecode-level), branches, cyclomatic complexity, lines, methods, classes.

## Excluding code from coverage

Configured in `build.gradle`:

```groovy
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                "com/importer/fileimporter/config/**",
                "com/importer/fileimporter/dto/**"
            ])
        }))
    }
}
```

## Current Known Coverage Gaps (re-verified 2026-09-12)

The previous version of this doc claimed a broad list of 0%-covered controllers/services/facades. Re-checked directly against `src/test` and `src/integration-test` — most of that list is now **covered**: `TransactionController` (via `TransactionControllerIntegrationSpec`), `CoinInformationService`, `HoldingService`, `PortfolioService`, `TransactionService`, `CoinInformationFacade` (three specs), `PortfolioDistributionFacade`. `BinanceIntegrationController` gained coverage 2026-09-12 (`BinanceIntegrationControllerSpec`) as part of the myTrades/allOrders windowing bugfix.

**What's genuinely still at 0% coverage:**

| Package | Classes | Missing tests |
|---|---|---|
| `config.security` / `controller.security` | `JwtAuthenticationFilter`, `JwtService`, `UserDetailsImpl`, `UserDetailsServiceImpl`, `AuthController` | Unit tests for JWT generation/validation, user loading; integration tests for `/api/auth/register` and `/api/auth/login` |
| `controller` | `HoldingController`, `PortfolioController`, `PricingController`, `WebController` | Integration tests for request/response shape, status codes |
| `service` | `CryptoCompareProxy`, `FileImporterService`, `PriceHistoryService`, `SymbolService` | Unit tests for business logic and edge cases |
| `facade` | `PricingFacade` | Unit tests for coordination logic |

The security package is the highest-value gap — it's the one area where a silent regression (e.g. a JWT validation bypass) would be a real security incident, not just an accounting error.

## Testing Conventions

- Spock + Groovy, `given/when/then` blocks (see [architecture.md](architecture.md) §3).
- Mock collaborators with `Mock()` for unit tests.
- Integration tests extend `BaseIntegrationSpec` (real Postgres via TestContainers).
- Complex behavior gets a scenario in [accounting-scenarios.md](accounting-scenarios.md) first.
