# Authentication Guide

How to register, log in, and make authenticated requests against InvestTracker. JWT-based.

## Default user

Created during database initialization:
- **Username**: `default_user`
- **Email**: `default@example.com`
- **Password**: `change_me` — change this immediately if this ever runs anywhere reachable.

## 1. Register

```bash
curl -X POST http://localhost:9080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
           "username": "investor_joe",
           "email": "joe@example.com",
           "password": "strongpassword123"
         }'
```

## 2. Login

```bash
curl -X POST http://localhost:9080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
           "username": "investor_joe",
           "password": "strongpassword123"
         }'
```

### Response

```json
{
  "jwt": "eyJhbGciOiJIUzI1NiJ9...",
  "id": null,
  "username": null,
  "email": null,
  "roles": null
}
```

> **Correction (2026-09-12):** the token field is `jwt`, not `token` — verified against a real login response. `id`/`username`/`email`/`roles` come back `null` on this response; don't rely on them (matches `importer-porfolio/CLAUDE.md`'s documented BE contract).

## 3. Using the token

Include it as a Bearer token on every subsequent request to a protected endpoint:

```bash
curl -X GET http://localhost:9080/holding \
     -H "Authorization: Bearer <YOUR_JWT>"
```

> **Correction (2026-09-12):** examples in the old version of this guide used port **8080** and endpoint `/api/holdings` — the app actually runs on **9080** (`application.yml`), and the real holdings endpoint is `/holding` (see [architecture.md](architecture.md) for the full corrected endpoint-path list — several docs independently repeated the same two wrong values).

### Postman
1. **Authorization** tab → Type: `Bearer Token`.
2. Paste the token from the login response.
3. Or: set an environment variable `jwt_token` via `pm.environment.set("jwt_token", pm.response.json().jwt)` in the login request's **Tests** tab (note: `.jwt`, not `.token`), then reference `{{jwt_token}}` elsewhere.
