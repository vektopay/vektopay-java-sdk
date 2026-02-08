package com.vektopay.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class VektopayClient {
    private final String apiKey;
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public VektopayClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public JsonNode createCharge(Map<String, Object> input) throws IOException, InterruptedException {
        return post("/v1/charges", input, input.getOrDefault("idempotencyKey", null));
    }

    public JsonNode createTransaction(Map<String, Object> input) throws IOException, InterruptedException {
        return post("/v1/transactions", input, null);
    }

    public JsonNode createCustomer(Map<String, Object> input) throws IOException, InterruptedException {
        return post("/v1/customers", input, null);
    }

    public JsonNode updateCustomer(String id, Map<String, Object> input) throws IOException, InterruptedException {
        return put("/v1/customers/" + id, input);
    }

    public JsonNode listCustomers(Map<String, Object> query) throws IOException, InterruptedException {
        return get("/v1/customers" + buildQuery(query));
    }

    public JsonNode getCustomer(String id) throws IOException, InterruptedException {
        return get("/v1/customers/" + id);
    }

    public JsonNode deleteCustomer(String id) throws IOException, InterruptedException {
        return del("/v1/customers/" + id);
    }

    public CheckoutSessionResponse createCheckoutSession(Map<String, Object> input) throws IOException, InterruptedException {
        JsonNode payload = post("/v1/checkout-sessions", input, null);
        return new CheckoutSessionResponse(
                payload.path("id").asText(),
                payload.path("token").asText(),
                payload.path("expires_at").asText()
        );
    }

    public ChargeStatusResponse pollChargeStatus(String transactionId, long intervalMs, long timeoutMs) throws IOException, InterruptedException {
        long startedAt = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - startedAt > timeoutMs) {
                throw new RuntimeException("poll_timeout");
            }
            JsonNode payload = get("/v1/charges/" + transactionId + "/status");
            String status = payload.path("status").asText();
            if ("PAID".equals(status) || "FAILED".equals(status)) {
                return new ChargeStatusResponse(payload.path("id").asText(), status);
            }
            Thread.sleep(intervalMs);
        }
    }

    private JsonNode post(String path, Map<String, Object> body, Object idempotencyKey) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));

        if (idempotencyKey != null) {
            builder.header("idempotency-key", idempotencyKey.toString());
        }

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode payload = mapper.readTree(response.body());
        if (response.statusCode() >= 300) {
            throw new RuntimeException(resolveError(payload, response.statusCode()));
        }
        return payload;
    }

    private JsonNode put(String path, Map<String, Object> body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode payload = mapper.readTree(response.body());
        if (response.statusCode() >= 300) {
            throw new RuntimeException(resolveError(payload, response.statusCode()));
        }
        return payload;
    }

    private JsonNode get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode payload = mapper.readTree(response.body());
        if (response.statusCode() >= 300) {
            throw new RuntimeException(resolveError(payload, response.statusCode()));
        }
        return payload;
    }

    private JsonNode del(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .DELETE()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode payload = mapper.readTree(response.body());
        if (response.statusCode() >= 300) {
            throw new RuntimeException(resolveError(payload, response.statusCode()));
        }
        return payload;
    }

    private String buildQuery(Map<String, Object> query) {
        if (query == null || query.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (entry.getValue() == null) continue;
            if (first) {
                sb.append("?");
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(encode(entry.getValue().toString()));
        }
        return sb.toString();
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String resolveError(JsonNode payload, int status) {
        JsonNode err = payload.path("error");
        if (err.isTextual()) return err.asText();
        if (err.has("message")) return err.path("message").asText();
        if (err.has("code")) return err.path("code").asText();
        return "request_failed_" + status;
    }

    public record CheckoutSessionResponse(String id, String token, String expiresAt) {}
    public record ChargeStatusResponse(String id, String status) {}
}
