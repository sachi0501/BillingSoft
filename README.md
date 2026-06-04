# BillingSoft

Spring Boot REST API for billing with PostgreSQL. It supports products, customers, invoices, invoice items, stock deduction, payments, and invoice cancellation rules.

The persistence layer uses Spring Data JPA with Hibernate ORM and PostgreSQL.

## Requirements

- Java 17
- PostgreSQL
- Maven wrapper is included (`mvnw.cmd`)

## Database Setup

The app is configured for your Aiven PostgreSQL database:

```text
Host: astranexis-billingsoft-sachidanand-aiven-2023.i.aivencloud.com
Port: 22261
Database: defaultdb
User: avnadmin
SSL mode: require
```

Set the password in your shell before starting the app. Do not commit the password into source control.

```powershell
$env:DB_PASSWORD="your-aiven-password"
.\mvnw.cmd spring-boot:run
```

You can override all connection values with environment variables:

```properties
DB_URL=jdbc:postgresql://astranexis-billingsoft-sachidanand-aiven-2023.i.aivencloud.com:22261/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=your-aiven-password
DDL_AUTO=update
```

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:8080
```

## Deploy On Render

This project includes a Dockerfile for Render.

Render setup:

```text
Service type: Web Service
Runtime: Docker
Dockerfile path: ./Dockerfile
```

Render will provide the `PORT` environment variable automatically. The Dockerfile starts Spring Boot with that port.

Recommended Render environment variables:

```properties
DDL_AUTO=update
```

After deployment, test:

```bash
curl -X GET "https://your-render-service.onrender.com/api/products"
```

## Product Endpoints

```text
POST   /api/products
GET    /api/products?active=true&search=paper
GET    /api/products/{id}
PUT    /api/products/{id}
PATCH  /api/products/{id}/stock
DELETE /api/products/{id}
```

Create product:

```json
{
  "sku": "SKU-100",
  "name": "Thermal Paper Roll",
  "description": "57mm billing roll",
  "unitPrice": 100,
  "taxRate": 18,
  "stockQuantity": 50,
  "active": true
}
```

Update stock:

```json
{
  "stockQuantity": 45
}
```

## Customer Endpoints

```text
POST   /api/customers
GET    /api/customers?active=true&search=retail
GET    /api/customers/{id}
PUT    /api/customers/{id}
DELETE /api/customers/{id}
```

Create customer:

```json
{
  "name": "Astra Retail",
  "email": "billing@example.com",
  "phone": "9999999999",
  "address": "Main Market",
  "taxNumber": "GSTIN123",
  "active": true
}
```

## Invoice Endpoints

```text
POST  /api/invoices
GET   /api/invoices?status=UNPAID&customerId=1
GET   /api/invoices/{id}
PATCH /api/invoices/{id}/cancel
POST  /api/invoices/{id}/payments
GET   /api/invoices/{id}/payments
```

Create invoice:

```json
{
  "customerId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "discountAmount": 10,
  "notes": "Counter sale",
  "dueDate": "2026-06-30"
}
```

Creating an invoice deducts product stock and calculates subtotal, tax total, discount, grand total, paid amount, balance due, and status.

## Payment Endpoints

```text
GET  /api/payments
POST /api/invoices/{id}/payments
```

Add payment:

```json
{
  "amount": 100,
  "paymentMethod": "CASH",
  "referenceNumber": "CASH-001",
  "notes": "First payment"
}
```

Payment methods:

```text
CASH, CARD, UPI, BANK_TRANSFER, CHEQUE, OTHER
```

Invoice statuses:

```text
UNPAID, PARTIALLY_PAID, PAID, CANCELLED
```

## Test

Tests use H2 in PostgreSQL compatibility mode:

```powershell
.\mvnw.cmd test
```
