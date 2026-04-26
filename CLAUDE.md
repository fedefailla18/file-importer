# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

InvestTracker (`file-importer`) is a Spring Boot application built for crypto investors to manage and track their portfolios. It specializes in historical transaction ingestion, precise cost-basis accounting, and tracking realized/unrealized profit/loss across market cycles.

## Build & Run Commands

```bash
./gradlew build              # Full build (compile + test + package)
./gradlew bootRun            # Run locally on port 9080
./gradlew test               # Unit tests only
./gradlew integrationTest    # Integration tests only (requires Docker)
./gradlew check              # All checks: unit + integration + coverage
```

**Authentication (JWT):**
```bash
# Register
curl -X POST http://localhost:9080/api/auth/register -H "Content-Type: application/json" -d '{"username":"user","email":"user@mail.com","password":"pwd"}'
# Login
curl -X POST http://localhost:9080/api/auth/login -H "Content-Type: application/json" -d '{"username":"user","password":"pwd"}'
# Authenticated Request
curl -H "Authorization: Bearer <TOKEN>" http://localhost:9080/api/holdings
```

**Single test:**
```bash
./gradlew test --tests com.importer.fileimporter.facade.CoinInformationFacadeSpec
# Specific method (escape spaces):
./gradlew test --tests '*CoinInformationFacadeSpec.should\ calculate\ transaction\ information*'
```

**Coverage reports:**
```bash
./gradlew jacocoAllTestReport   # Combined unit + integration coverage
# Reports at: build/reports/jacoco/jacocoAllTestReport/html/index.html
```

**Swagger UI:** `http://localhost:9080/swagger-ui.html`

## Local Dev with Docker

```bash
cd docker && ./start-db.sh   # Spins up PostgreSQL 13.1 on port 5435
```

The script tears down existing containers, cleans the data volume, and rebuilds. Database: `importer_database`, schema: `file_importer_schema`.

## Architecture

The app uses a strict layered architecture:

```
Controller → Facade → Service → Repository → Entity (JPA)
```

- **Controllers** (`/controller`): REST endpoints with SpringDoc/OpenAPI annotations
- **Facades** (`/facade`): Orchestration layer — coordinate multiple services; e.g., `CoinInformationFacade` aggregates holdings + transactions
- **Services** (`/service`): Core business logic. Key ones:
  - `HoldingService` — recalculates holdings from transactions
  - `CoinInformationService` — aggregates per-coin stats
  - `FileImporterService` — parses CSV/Excel uploads
  - `CryptoCompareProxy` — external pricing API integration
- **Repositories** (`/repository`): Spring Data JPA
- **Adapters** (`/dto/adapter`): Exchange-specific transaction parsers (`BinanceTransactionAdapter`, `MexcTransactionAdapter`)

**Factory pattern** is used for plugging in exchange adapters: `TransactionAdapterFactory` and `ProcessFileFactory`.

## Testing

Tests are written in **Groovy + Spock** (not JUnit directly):
- Unit tests: `src/test/groovy/` — extend `Specification`, use `Mock()` and given/when/then blocks
- Integration tests: `src/integration-test/groovy/` — extend `BaseIntegrationSpec`, which starts a real PostgreSQL container via TestContainers

Coverage targets: 80% line, 70% branch. Config/DTO packages are excluded from JaCoCo.

## Database Migrations

Managed by **Liquibase**. Changelogs live in `src/main/resources/db/changelog/`. The master file is `db.changelog-master.xml`; individual changes go in `changes/`. Contexts: `development`, `production`.

## Key Config

`src/main/resources/application.yml` — database, JWT settings, Swagger paths, CryptoCompare API key, and file upload limits (10MB/50MB). Credentials in this file and `docker/docker-compose.yml` are hardcoded for local dev and should be externalized via environment variables before any production deployment.

## Binance Integration

The Binance sync feature is fully implemented. Key classes:

- **`ExchangeConfigController`** (`/api/exchange`) — `POST /config` saves/updates keys (secret AES-encrypted via `EncryptionService`); `GET /config` returns configs with masked secret (never returned).
- **`BinanceSyncService`** — orchestrates the sync: decrypts secret, calls `BinanceApiService` for account info + exchange info + trades per symbol, adapts each trade via `BinanceApiTransactionAdapter`, and processes via `TransactionProcessor`. Updates `lastSyncTimestamp` on `UserExchangeConfig` after a successful run.
- **`BinanceApiService`** — Spring `WebClient`-based; signs requests with HMAC-SHA256.
- **`UserExchangeConfig`** entity — keyed by `(user, exchangeName)`. `ExchangeName` is an enum; only `BINANCE` is currently used.
- **Trigger**: `POST /transaction/sync/binance?portfolio=<name>` (in `TransactionController`).
- **DB migration**: `db/changelog/changes/2026-04-25-035-add-exchange-config.sql`.

If a user has no Binance config saved, `BinanceSyncService.sync` throws `IllegalArgumentException("Binance API keys not configured for user")` — the FE checks for this string in the error response.

## API Documentation

Follow the patterns in `docs/api-documentation-guide.md`. Use `@Tag`, `@Operation`, `@ApiResponse`, and `@Parameter` annotations from SpringDoc. `TransactionController` is the reference example.
