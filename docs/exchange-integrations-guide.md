# Exchange Integrations API Guide

This guide provides engineering documentation for integrating with the InvestTracker portfolio synchronization services. It details the unified data model, supported exchanges, and the workflow for fetching transactions and portfolio data.

## 1. Overview
InvestTracker acts as a centralized hub for multiple exchanges (Binance, MexC, IOL). It abstracts away exchange-specific API complexities (rate limiting, signing, token management) and provides a unified representation of your financial activity.

## 2. Supported Exchanges

| Exchange | Auth Method | Sync Types | Implementation |
|----------|-------------|------------|----------------|
| **Binance** | API Key / Secret | Trades, Deposits, Withdrawals, Fiat, Convert | Native WebClient + Staging |
| **MexC** | API Key / Secret | Trades, Deposits, Withdrawals | Native WebClient + Staging |
| **IOL** (InvertirOnline) | OAuth2 (User/Pass) | Balances, Portfolio, Operations | Feign Client + Caching |

## 3. Unified Data Model

Regardless of the source exchange, data is normalized into three core entities:

### 3.1 Transaction
Represents an atomic movement of funds.
- `side`: BUY, SELL, DEPOSIT, WITHDRAW.
- `symbol`: The primary asset (e.g., BTC).
- `pair`: The trading pair (e.g., BTCUSDT).
- `executed`: Quantity of the asset.
- `price`: Price in the quote asset.
- `paidAmount`: Total cost in the quote asset.

### 3.2 Holding
Calculated state of an asset within a portfolio.
- `totalAmount`: Current balance.
- `avgCostBasis`: Average price paid (weighted).
- `realizedProfit`: Total profit/loss from closed positions.

### 3.3 Portfolio
A logical grouping of transactions and holdings (e.g., "MyRetirement", "ActiveTrading").

## 4. API Workflow

### Step 1: Authentication
All requests to InvestTracker must include a JWT token in the `Authorization` header.
[See Authentication Guide](authentication-guide.md).

### Step 2: Configure Exchange Credentials
Before syncing, save the exchange credentials for the user.
- **Endpoint:** `POST /api/exchange/config`
- **Payload:**
```json
{
  "exchangeName": "BINANCE",
  "apiKey": "...",
  "apiSecret": "..."
}
```
*Note: Saving credentials automatically creates a corresponding portfolio (e.g., "BINANCE").*

### Step 3: Synchronize Data
Trigger a background synchronization of historical data.
- **Binance:** `POST /transaction/sync/binance/full?portfolio=MyPortfolio`
- **MexC:** `POST /transaction/sync/mexc/full?portfolio=MyPortfolio`
- **IOL:** Direct fetching is currently supported (Real-time).

### Step 4: Fetch Results
- **Transactions:** `GET /transaction/filter?portfolioName=MyPortfolio`
- **Holdings:** `GET /api/holdings`
- **Portfolio Value:** `POST /api/portfolio/distribution`

## 5. Architecture & Best Practices

### Staging Areas
For high-volume exchanges (Binance, MexC), we use "Raw Order" staging tables. This allows us to re-process transactions without hitting the external API rate limits again.

### Rate Limiting & Resilience
- **Binance/MexC:** Managed via defensive delays and weight tracking in the Service layer.
- **IOL:** Managed via an `ErrorDecoder` and automatic token caching (Caffeine) in the Feign client layer.

### Adding New Exchanges
1. Add to `ExchangeName` enum.
2. Implement `RawOrder` interface if using staging.
3. Use `FeignClient` for new integrations where possible.
4. Update `TransactionProcessor` if new transaction types are introduced.

## 6. Real-time IOL Endpoints
IOL data is fetched in real-time to ensure accurate balance reporting:
- `GET /api/integration/iol/account-statement`
- `GET /api/integration/iol/portfolio/{country}`
- `GET /api/integration/iol/operations`
