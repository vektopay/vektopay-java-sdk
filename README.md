# Vektopay Java SDK

MVP: charges + checkout sessions + polling.

## Install (Gradle)

```gradle
dependencies {
  implementation "com.vektopay:vektopay-java-sdk:0.1.0"
}
```

## Usage

```java
VektopayClient client = new VektopayClient(
  System.getenv("VEKTOPAY_API_KEY"),
  "https://api.vektopay.com"
);

VektopayClient.CheckoutSessionResponse session = client.createCheckoutSession(Map.of(
  "customer_id", "cust_123",
  "amount", 1000,
  "currency", "BRL"
));
```

## Build

```bash
./gradlew build
```
