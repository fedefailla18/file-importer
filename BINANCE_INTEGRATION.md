# Product & Engineering Document: Binance API Integration

## 1. Executive Summary
The Binance API Integration enables InvestTracker users to automatically synchronize their spot trading history directly from Binance. This feature eliminates the manual burden of exporting and uploading CSV files, ensuring portfolio data is always up-to-date with minimal user effort.

## 2. Product Details

### 2.1 User Experience
- **Credential Management**: Users can securely save their Binance API Key and Secret Key via the `/api/exchange/config` endpoint.
- **One-Click Sync**: A new synchronization trigger (`/transaction/sync/binance`) allows users to fetch all historical and recent trades into a specified portfolio.
- **Incremental Updates**: The system tracks the `lastSyncTimestamp` to ensure subsequent syncs only fetch new transactions, saving time and API quota.
- **Fresh Activity Viewer**: A read-only endpoint and UI page expose fresh balances and raw spot trades directly from Binance so users can compare Binance output against InvestTracker accounting.

### 2.2 Functional Requirements
- Secure storage of API credentials.
- Automatic discovery of traded pairs based on account balances.
- Mapping of Binance API trade data to the internal InvestTracker accounting engine.
- De-duplication of transactions (handled by the existing `TransactionProcessor`).

## 3. Software Engineering Details

### 3.1 Architectural Overview
The integration follows the established layered architecture of InvestTracker:
- **Controller**: `ExchangeConfigController` and `TransactionController` extensions.
- **Service**: `BinanceSyncService` (Orchestration) and `BinanceApiService` (External Communication).
- **Security**: `EncryptionService` for AES encryption of secrets.
- **Adapter**: `BinanceApiTransactionAdapter` for data normalization.

### 3.2 Security Design
- **Encryption at Rest**: Secret Keys are encrypted using AES/ECB/PKCS5Padding before being stored in the database.
- **HMAC SHA256 Signing**: All authenticated requests to Binance are signed using the user's Secret Key as per Binance API security requirements.
- **Credential Masking**: API Secrets are never returned via GET endpoints; only the API Key and sync metadata are visible.

### 3.3 Data Flow: Synchronization Process
1. **Authentication**: Retrieve and decrypt API credentials for the current user.
2. **Asset Discovery**: Call `GET /api/v3/account` to identify assets with non-zero balances.
3. **Pair Mapping**: Call `GET /api/v3/exchangeInfo` to find valid trading pairs for identified assets.
4. **Trade Ingestion**: Iterate through pairs and call `GET /api/v3/myTrades` using the `lastSyncTimestamp`.
5. **Normalization**: Convert `BinanceTradeResponse` objects to `Transaction` entities via the `BinanceApiTransactionAdapter`.
6. **Processing**: Pass entities to `TransactionProcessor` for cost-basis calculation and holding updates.

### 3.4 API Endpoints

#### Configuration
- `POST /api/exchange/config`:
    - Request: `{ "exchangeName": "BINANCE", "apiKey": "...", "apiSecret": "..." }`
- `GET /api/exchange/config`:
    - Response: `[{ "exchangeName": "BINANCE", "apiKey": "...", "lastSyncTimestamp": ... }]`

#### Synchronization
- `POST /transaction/sync/binance?portfolio={name}`:
    - Triggers the full account sync for the authenticated user.

#### Fresh Activity
- `GET /api/exchange/binance/spot-activity`:
    - Returns fresh spot balances and raw trade rows from Binance for the authenticated user.
    - Intended for reconciliation and transparency, not as the accounting source of truth.

## 4. Technical Stack
- **HTTP Client**: Spring WebClient (Reactive/Non-blocking).
- **Encryption**: Java Cryptography Extension (JCE).
- **Database**: PostgreSQL with Liquibase migrations.
- **Testing**: JUnit 5 and Spock (existing project standard).
