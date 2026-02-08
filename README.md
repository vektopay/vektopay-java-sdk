# Vektopay Java SDK

Java SDK for Vektopay API (server-side). Supports payments, checkout sessions, and payment status polling.

## Install (Gradle)

```gradle
dependencies {
  implementation "com.vektopay:vektopay-java-sdk:0.1.0"
}
```

## Setup

```java
VektopayClient client = new VektopayClient(
  System.getenv("VEKTOPAY_API_KEY"),
  "https://api.vektopay.com"
);
```

## Create Transaction (API Checkout)

```java
JsonNode transaction = client.createTransaction(Map.of(
  "customer_id", "cust_123",
  "items", List.of(
    Map.of("price_id", "price_basic", "quantity", 1)
  ),
  "coupon_code", "OFF10",
  "payment_method", Map.of(
    "type", "credit_card",
    "token", "ev:tk_123",
    "installments", 1
  )
));
```

## Create Customer

Customers must exist before creating transactions or charges.

```java
JsonNode customer = client.createCustomer(Map.of(
  "merchant_id", "mrc_123",
  "external_id", "cust_ext_123",
  "name", "Ana Silva",
  "email", "ana@example.com",
  "doc_type", "CPF",
  "doc_number", "12345678901"
));
```

## Update Customer

```java
JsonNode updated = client.updateCustomer(
  "cust_123",
  Map.of(
    "name", "Ana Maria Silva",
    "email", "ana.maria@example.com"
  )
);
```

## Get Customer

```java
JsonNode detail = client.getCustomer("cust_123");
```

## List Customers

```java
JsonNode customers = client.listCustomers(Map.of(
  "merchant_id", "mrc_123",
  "limit", 50,
  "offset", 0
));
```

## Delete Customer

```java
JsonNode deleted = client.deleteCustomer("cust_123");
```

## Create Charge (Card)

```java
JsonNode charge = client.createCharge(Map.of(
  "customer_id", "cust_123",
  "card_id", "card_123",
  "amount", 1000,
  "currency", "BRL",
  "installments", 1
));
```

## Create Checkout Session (Frontend)

```java
VektopayClient.CheckoutSessionResponse session = client.createCheckoutSession(Map.of(
  "customer_id", "cust_123",
  "amount", 1000,
  "currency", "BRL",
  "success_url", "https://example.com/success",
  "cancel_url", "https://example.com/cancel"
));
```

## Poll Charge Status

```java
VektopayClient.ChargeStatusResponse status = client.pollChargeStatus(
  "txn_123",
  3000,
  120000
);
```

## Build

```bash
./gradlew build
```

## Notes
- Never expose your API key in the browser.
