# Postman & API Documentation Guide

This document provides instructions for manual testing with Postman and documenting the API workflows for InvestTracker.

## 1. Environment Setup

To use these endpoints effectively in Postman, create an **Environment** with the following variables:
- `baseUrl`: `http://localhost:9080`
- `jwt_token`: (Leave empty, will be populated by the login request)

## 2. Authentication Workflow

### Step 1: Register (Optional)
```bash
curl -X POST "{{baseUrl}}/api/auth/register" \
     -H "Content-Type: application/json" \
     -d '{"username":"dev_user","email":"dev@example.com","password":"securePassword123"}'
```

### Step 2: Login & Save Token
In Postman, use the following `curl` for the login. 
**Pro Tip:** In the Postman **Tests** tab, add `pm.environment.set("jwt_token", pm.response.json().token);` to automatically update your environment.

```bash
curl -X POST "{{baseUrl}}/api/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username":"dev_user","password":"securePassword123"}'
```

---

## 3. Binance Integration Endpoints

### Step 3: Configure Binance Credentials
Securely store your API keys. They will be encrypted at rest.

```bash
curl -X POST "{{baseUrl}}/api/exchange/config" \
     -H "Authorization: Bearer {{jwt_token}}" \
     -H "Content-Type: application/json" \
     -d '{
       "exchangeName": "BINANCE",
       "apiKey": "YOUR_API_KEY",
       "apiSecret": "YOUR_SECRET_KEY"
     }'
```

### Step 4: Direct Query (Exhaustive Orders)
Fetch all historical orders for a specific symbol directly from Binance.

```bash
curl -X GET "{{baseUrl}}/api/integration/binance/orders?symbol=BTCUSDT" \
     -H "Authorization: Bearer {{jwt_token}}"
```

### Step 5: Full Portfolio Sync
Pull all historical trades, deposits, and withdrawals into a portfolio.

```bash
curl -X POST "{{baseUrl}}/transaction/sync/binance/full?portfolio=MainBinance" \
     -H "Authorization: Bearer {{jwt_token}}"
```

---

## 4. MexC Integration Endpoints

Similar to Binance, but for the MEXC exchange.

### Step 6: Configure MexC Credentials
```bash
curl -X POST "{{baseUrl}}/api/exchange/config" \
     -H "Authorization: Bearer {{jwt_token}}" \
     -H "Content-Type: application/json" \
     -d '{
       "exchangeName": "MEXC",
       "apiKey": "YOUR_API_KEY",
       "apiSecret": "YOUR_SECRET_KEY"
     }'
```

### Step 7: Full Portfolio Sync (MexC)
```bash
curl -X POST "{{baseUrl}}/transaction/sync/mexc/full?portfolio=MainMexC" \
     -H "Authorization: Bearer {{jwt_token}}"
```

---

## 5. Portfolio & Accounting Analysis

### View Holdings
Check your calculated cost-basis and balances after sync.

```bash
curl -X GET "{{baseUrl}}/api/holdings" \
     -H "Authorization: Bearer {{jwt_token}}"
```

### Portfolio Distribution
Get the total value in USDT and BTC.

```bash
curl -X POST "{{baseUrl}}/api/portfolio/distribution" \
     -H "Authorization: Bearer {{jwt_token}}"
```

---

## 5. Future E2E Automation
This document serves as the specification for our automation suite.
- **Tools**: Newman (Postman CLI) + GitHub Actions.
- **Workflow**: 
  1. Spin up Docker database.
  2. Run Migration.
  3. Execute Newman collection targeting the `integration-test` profile.
  4. Validate database state and JSON responses.
