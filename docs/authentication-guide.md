# Authentication Guide

This document provides instructions on how to register, login, and perform authenticated requests in InvestTracker.

## Overview

InvestTracker uses JWT (JSON Web Token) for authentication. 
- **Registration**: Create a new account.
- **Login**: Exchange credentials for a JWT token.
- **Authenticated Requests**: Use the JWT token in the `Authorization` header.

## Default User

A default user is created during database initialization:
- **Username**: `default_user`
- **Email**: `default@example.com`
- **Password**: `change_me` (Note: In a production environment, this should be changed immediately).

---

## 1. Register a New User

If you don't have a user account, you can register one.

### Endpoint
`POST /api/auth/register`

### Payload
```json
{
  "username": "investor_joe",
  "email": "joe@example.com",
  "password": "strongpassword123"
}
```

### CURL
```bash
curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
           "username": "investor_joe",
           "email": "joe@example.com",
           "password": "strongpassword123"
         }'
```

---

## 2. Login

Authenticate to receive your JWT token.

### Endpoint
`POST /api/auth/login`

### Payload
```json
{
  "username": "investor_joe",
  "password": "strongpassword123"
}
```

### CURL
```bash
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
           "username": "investor_joe",
           "password": "strongpassword123"
         }'
```

### Expected Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 3. Using the Token (Postman/CURL)

Once you have the token, you must include it in all subsequent requests to protected endpoints.

### Postman
1. Go to the **Authorization** tab.
2. Select **Type**: `Bearer Token`.
3. Paste your token in the **Token** field.

### CURL Example (Get Holdings)
Replace `<YOUR_TOKEN>` with the token received from the login response.

```bash
curl -X GET http://localhost:8080/api/holdings \
     -H "Authorization: Bearer <YOUR_TOKEN>"
```

---

## Authentication Flow in Postman

1. **Register** (optional): Use the Register CURL.
2. **Login**: Use the Login CURL to get the token.
3. **Set Variable**: In Postman, you can set an Environment Variable `jwt_token`.
4. **Use Variable**: In other requests, set Authorization to `Bearer {{jwt_token}}`.
