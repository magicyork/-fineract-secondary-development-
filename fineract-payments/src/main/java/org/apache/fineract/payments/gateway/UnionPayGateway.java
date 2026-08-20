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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.fineract.payments.config.UnionPayProperties;
import org.apache.fineract.payments.data.UnionPayRequest;
import org.apache.fineract.payments.data.UnionPayResponse;
import org.apache.fineract.payments.data.UnionPayTransferRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayGateway implements PaymentGateway {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final UnionPayProperties properties;
    private final Gson gson = new GsonBuilder().create();

    @Override
    public UnionPayResponse execute(UnionPayRequest request) {
        if (!isEnabled()) {
            throw new IllegalStateException("UnionPay payment is not enabled");
        }

        try {
            Map<String, Object> params = buildRequestParams(request);
            String sign = generateSignature(params);
            params.put("sign", sign);

            String jsonPayload = gson.toJson(params);
            log.info("UnionPay request: orderId={}, amount={}", request.getOrderId(), request.getAmount());

            RequestBody body = RequestBody.create(jsonPayload, JSON_TYPE);
            Request httpRequest = new Request.Builder().url(properties.getGatewayUrl()).post(body)
                    .addHeader("Content-Type", "application/json").build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                String responseJson = responseBody != null ? responseBody.string() : "";
                log.info("UnionPay response: {}", responseJson);

                if (!response.isSuccessful()) {
                    log.error("UnionPay HTTP error: {}", response.code());
                    return buildErrorResponse("HTTP_" + response.code(), "HTTP request failed");
                }

                return gson.fromJson(responseJson, UnionPayResponse.class);
            }

        } catch (Exception e) {
            log.error("UnionPay payment exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    @Override
    public UnionPayResponse query(String orderId) {
        return buildErrorResponse("NOT_IMPLEMENTED", "Query interface not implemented");
    }

    @Override
    public UnionPayResponse reverse(String orderId) {
        return buildErrorResponse("NOT_IMPLEMENTED", "Reverse interface not implemented");
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private Map<String, Object> buildRequestParams(UnionPayRequest request) {
        Map<String, Object> params = new TreeMap<>();
        params.put("merchantId", properties.getMerchantId());
        params.put("orderId", request.getOrderId());
        params.put("amount", request.getAmount().multiply(new BigDecimal("100")).longValue());
        params.put("currencyCode", request.getCurrencyCode());
        params.put("transType", request.getTransType());
        params.put("notifyUrl", properties.getNotifyUrl());

        if (request.getCardNo() != null) {
            params.put("cardNo", encrypt(request.getCardNo()));
        }
        if (request.getCvv2() != null) {
            params.put("cvv2", encrypt(request.getCvv2()));
        }
        if (request.getExpiryDate() != null) {
            params.put("expiryDate", request.getExpiryDate());
        }
        if (request.getCardHolderName() != null) {
            params.put("cardHolderName", request.getCardHolderName());
        }
        if (request.getIdNo() != null) {
            params.put("idNo", encrypt(request.getIdNo()));
        }
        if (request.getMobileNo() != null) {
            params.put("mobileNo", request.getMobileNo());
        }

        return params;
    }

    private String generateSignature(Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            sb.append("key=").append(properties.getSecretKey());

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString().toUpperCase(Locale.ROOT);

        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    private String encrypt(String data) {
        return data;
    }

    public UnionPayResponse transfer(UnionPayTransferRequest request) {
        if (!isEnabled()) {
            throw new IllegalStateException("UnionPay is not enabled");
        }

        try {
            Map<String, Object> params = buildTransferParams(request);
            String sign = generateSignature(params);
            params.put("sign", sign);

            String jsonPayload = gson.toJson(params);
            log.info("UnionPay transfer request: orderId={}, amount={}, cardNo={}", request.getOrderId(), request.getAmount(),
                    maskCard(request.getCardNo()));

            RequestBody body = RequestBody.create(jsonPayload, JSON_TYPE);
            Request httpRequest = new Request.Builder().url(properties.getGatewayUrl()).post(body)
                    .addHeader("Content-Type", "application/json").build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                String responseJson = responseBody != null ? responseBody.string() : "";
                log.info("UnionPay transfer response: {}", responseJson);

                if (!response.isSuccessful()) {
                    log.error("UnionPay transfer HTTP error: {}", response.code());
                    return buildErrorResponse("HTTP_" + response.code(), "Transfer HTTP request failed");
                }

                return gson.fromJson(responseJson, UnionPayResponse.class);
            }

        } catch (Exception e) {
            log.error("UnionPay transfer exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    private Map<String, Object> buildTransferParams(UnionPayTransferRequest request) {
        Map<String, Object> params = new TreeMap<>();
        params.put("merchantId", properties.getMerchantId());
        params.put("orderId", request.getOrderId());
        params.put("amount", request.getAmount().multiply(new BigDecimal("100")).longValue());
        params.put("currencyCode", request.getCurrencyCode() != null ? request.getCurrencyCode() : "CNY");
        params.put("transType", request.getTransType() != null ? request.getTransType() : "TRANSFER");
        params.put("notifyUrl", properties.getNotifyUrl());

        if (request.getCardNo() != null) {
            params.put("cardNo", encrypt(request.getCardNo()));
        }
        if (request.getCardHolderName() != null) {
            params.put("cardHolderName", request.getCardHolderName());
        }
        if (request.getIdNo() != null) {
            params.put("idNo", encrypt(request.getIdNo()));
        }
        if (request.getMobileNo() != null) {
            params.put("mobileNo", request.getMobileNo());
        }

        return params;
    }

    private String maskCard(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + "********" + cardNo.substring(cardNo.length() - 4);
    }

    private UnionPayResponse buildErrorResponse(String code, String msg) {
        UnionPayResponse response = new UnionPayResponse();
        response.setRespCode(code);
        response.setRespMsg(msg);
        return response;
    }
}
