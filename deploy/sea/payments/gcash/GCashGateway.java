/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.payments.gateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.fineract.payments.config.GCashProperties;
import org.apache.fineract.payments.data.GCashPaymentRequest;
import org.apache.fineract.payments.data.GCashPaymentResponse;
import org.springframework.stereotype.Component;

/**
 * GCash Payment Gateway Implementation for Philippines market.
 *
 * GCash is a mobile payment service by Globe Telecom in partnership with Alipay.
 * This gateway handles:
 *  - In-app payment (APP_PAY)
 *  - QR code payment (QR_PAY)
 *  - Fund transfer (TRANSFER)
 *  - Payment status query (QUERY)
 *  - Payment reversal (REVERSE)
 *
 * API Documentation: https://gcash.gitbook.io/gcash-open-api/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GCashGateway {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final OkHttpClient okHttpClient;
    private final GCashProperties properties;
    private final Gson gson = new GsonBuilder().create();

    /**
     * Execute GCash payment (In-App or QR code payment).
     */
    public GCashPaymentResponse execute(GCashPaymentRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("GCash payment is not enabled");
        }

        try {
            Map<String, Object> params = buildPaymentParams(request);
            String payload = gson.toJson(params);
            String signature = generateSignature(payload);

            log.info("GCash payment request: orderId={}, amount={}, paymentType={}",
                    request.getOrderId(), request.getAmount(), request.getPaymentType());

            Request httpRequest = new Request.Builder()
                    .url(properties.getGatewayUrl() + "/payment/do)
                    .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-GCASH-PARTNER-ID", properties.getPartnerId())
                    .addHeader("X-GCASH-TIMESTAMP", String.valueOf(Instant.now().toEpochMilli()))
                    .addHeader("X-GCASH-SIGNATURE", signature)
                    .build();

            return executeAndProcess(httpRequest);

        } catch (Exception e) {
            log.error("GCash payment exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    /**
     * Query payment status.
     */
    public GCashPaymentResponse query(String orderId) {
        try {
            Map<String, Object> params = new TreeMap<>();
            params.put("partnerId", properties.getPartnerId());
            params.put("orderId", orderId);
            params.put("timestamp", Instant.now().toEpochMilli());

            String payload = gson.toJson(params);
            String signature = generateSignature(payload);

            Request httpRequest = new Request.Builder()
                    .url(properties.getGatewayUrl() + "/payment/query")
                    .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-GCASH-PARTNER-ID", properties.getPartnerId())
                    .addHeader("X-GCASH-SIGNATURE", signature)
                    .build();

            return executeAndProcess(httpRequest);

        } catch (Exception e) {
            log.error("GCash query exception for orderId={}", orderId, e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    /**
     * Reverse a GCash payment.
     */
    public GCashPaymentResponse reverse(String orderId) {
        try {
            Map<String, Object> params = new TreeMap<>();
            params.put("partnerId", properties.getPartnerId());
            params.put("orderId", orderId);
            params.put("timestamp", Instant.now().toEpochMilli());
            params.put("reason", "REVERSED_BY_MERCHANT");

            String payload = gson.toJson(params);
            String signature = generateSignature(payload);

            Request httpRequest = new Request.Builder()
                    .url(properties.getGatewayUrl() + "/payment/reverse")
                    .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-GCASH-PARTNER-ID", properties.getPartnerId())
                    .addHeader("X-GCASH-SIGNATURE", signature)
                    .build();

            return executeAndProcess(httpRequest);

        } catch (Exception e) {
            log.error("GCash reverse exception for orderId={}", orderId, e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    /**
     * Transfer funds to another GCash user or bank account.
     */
    public GCashPaymentResponse transfer(GCashPaymentRequest request) {
        try {
            Map<String, Object> params = buildTransferParams(request);
            String payload = gson.toJson(params);
            String signature = generateSignature(payload);

            log.info("GCash transfer request: orderId={}, amount={}, receiver={}",
                    request.getOrderId(), request.getAmount(), maskPhone(request.getReceiverPhone()));

            Request httpRequest = new Request.Builder()
                    .url(properties.getGatewayUrl() + "/transfer/do")
                    .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-GCASH-PARTNER-ID", properties.getPartnerId())
                    .addHeader("X-GCASH-SIGNATURE", signature)
                    .build();

            return executeAndProcess(httpRequest);

        } catch (Exception e) {
            log.error("GCash transfer exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String getType() {
        return "GCASH";
    }

    // ---- Private Helper Methods ----

    private Map<String, Object> buildPaymentParams(GCashPaymentRequest request) {
        Map<String, Object> params = new TreeMap<>();
        params.put("partnerId", properties.getPartnerId());
        params.put("orderId", request.getOrderId());
        params.put("amount", request.getAmount());
        params.put("currency", "PHP");
        params.put("description", request.getDescription());
        params.put("paymentType", request.getPaymentType());
        params.put("timestamp", Instant.now().toEpochMilli());

        if (request.getCallbackUrl() != null) {
            params.put("callbackUrl", request.getCallbackUrl());
        }
        if (request.getRedirectUrl() != null) {
            params.put("redirectUrl", request.getRedirectUrl());
        }
        if (request.getBuyerId() != null) {
            params.put("buyerId", request.getBuyerId());
        }
        if (request.getExtParams() != null) {
            params.put("extParams", request.getExtParams());
        }

        return params;
    }

    private Map<String, Object> buildTransferParams(GCashPaymentRequest request) {
        Map<String, Object> params = new TreeMap<>();
        params.put("partnerId", properties.getPartnerId());
        params.put("orderId", request.getOrderId());
        params.put("amount", request.getAmount());
        params.put("currency", "PHP");
        params.put("receiverPhone", request.getReceiverPhone());
        params.put("receiverName", request.getReceiverName());
        params.put("timestamp", Instant.now().toEpochMilli());

        if (request.getBankCode() != null) {
            params.put("bankCode", request.getBankCode());
        }
        if (request.getAccountName() != null) {
            params.put("accountName", request.getAccountName());
        }

        return params;
    }

    private String generateSignature(String payload) throws Exception {
        String signString = properties.getPartnerId() + payload;

        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(
                properties.getSsoKey().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(keySpec);

        byte[] hash = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private GCashPaymentResponse executeAndProcess(Request httpRequest) {
        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            ResponseBody responseBody = response.body();
            String responseJson = responseBody != null ? responseBody.string() : "";
            log.debug("GCash response: {}", responseJson);

            if (!response.isSuccessful()) {
                log.error("GCash HTTP error: code={}, body={}", response.code(), responseJson);
                return buildErrorResponse("HTTP_ERROR", "HTTP " + response.code());
            }

            GCashPaymentResponse gcashResponse = gson.fromJson(responseJson, GCashPaymentResponse.class);

            verifySignature(response, responseJson);

            return gcashResponse;

        } catch (Exception e) {
            log.error("GCash API call exception", e);
            return buildErrorResponse("NETWORK_ERROR", e.getMessage());
        }
    }

    private void verifySignature(Response response, String responseJson) {
        String signature = response.header("X-GCASH-SIGNATURE");
        if (signature != null && !signature.isEmpty()) {
            try {
                String signString = properties.getPartnerId() + responseJson;
                Mac mac = Mac.getInstance(HMAC_SHA256);
                SecretKeySpec keySpec = new SecretKeySpec(
                        properties.getSsoKey().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
                mac.init(keySpec);
                byte[] hash = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
                String expectedSignature = Base64.getEncoder().encodeToString(hash);

                if (!signature.equals(expectedSignature)) {
                    log.warn("GCash response signature verification failed");
                }
            } catch (Exception e) {
                log.error("GCash signature verification error", e);
            }
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    private GCashPaymentResponse buildErrorResponse(String code, String msg) {
        GCashPaymentResponse response = new GCashPaymentResponse();
        response.setResultCode(code);
        response.setResultMessage(msg);
        response.setSuccess(false);
        return response;
    }
}
