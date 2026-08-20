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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.fineract.payments.config.WeChatPayProperties;
import org.apache.fineract.payments.data.WeChatPayRequest;
import org.apache.fineract.payments.data.WeChatPayResponse;
import org.apache.fineract.payments.data.WeChatPayTransferRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatPayGateway {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final WeChatPayProperties properties;
    private final Gson gson = new GsonBuilder().create();

    public WeChatPayResponse execute(WeChatPayRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("WeChat Pay is not enabled");
        }

        try {
            Map<String, Object> body = buildRequestBody(request);
            String jsonPayload = gson.toJson(body);

            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);

            String signature = generateSignature(body, nonceStr, timeStamp);

            log.info("WeChat Pay request: outTradeNo={}, amount={}", request.getOutTradeNo(), request.getAmount());

            RequestBody requestBody = RequestBody.create(jsonPayload, JSON_TYPE);
            Request httpRequest = new Request.Builder().url(properties.getGatewayUrl() + "/v3/pay/transactions/native").post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization",
                            "WECHATPAY2-SHA256-RSA2048 mchid=\"" + properties.getMchId() + "\",nonce_str=\"" + nonceStr + "\",timestamp=\""
                                    + timeStamp + "\",serial_no=\"" + properties.getMchSerialNo() + "\",signature=\"" + signature + "\"")
                    .build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                String responseJson = responseBody != null ? responseBody.string() : "";
                log.info("WeChat Pay response: {}", responseJson);

                if (!response.isSuccessful()) {
                    log.error("WeChat Pay HTTP error: {}", response.code());
                    return buildErrorResponse("HTTP_ERROR", "HTTP request failed");
                }

                return gson.fromJson(responseJson, WeChatPayResponse.class);
            }

        } catch (Exception e) {
            log.error("WeChat Pay payment exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    public String getType() {
        return "WECHATPAY";
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private Map<String, Object> buildRequestBody(WeChatPayRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("appid", properties.getAppId());
        body.put("mchid", properties.getMchId());
        body.put("description", request.getDescription());
        body.put("out_trade_no", request.getOutTradeNo());
        body.put("notify_url", properties.getNotifyUrl());

        Map<String, Object> amount = new HashMap<>();
        int totalFen = request.getAmount().multiply(new BigDecimal("100")).intValue();
        amount.put("total", totalFen);
        amount.put("currency", "CNY");
        body.put("amount", amount);

        if (request.getOpenid() != null) {
            Map<String, Object> payer = new HashMap<>();
            payer.put("openid", request.getOpenid());
            body.put("payer", payer);
        }

        return body;
    }

    private String generateSignature(Map<String, Object> body, String nonceStr, String timeStamp) {
        try {
            String message = properties.getMchId() + "\n" + "POST\n" + "/v3/pay/transactions/native\n" + timeStamp + "\n" + nonceStr + "\n"
                    + gson.toJson(body) + "\n";

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(message.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("WeChat Pay signature generation failed", e);
        }
    }

    public WeChatPayResponse transfer(WeChatPayTransferRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("WeChat Pay is not enabled");
        }

        try {
            Map<String, Object> body = buildTransferRequestBody(request);
            String jsonPayload = gson.toJson(body);

            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);

            String signature = generateTransferSignature(body, nonceStr, timeStamp);

            log.info("WeChat Pay transfer request: outTransferNo={}, amount={}, openid={}", request.getOutTransferNo(), request.getAmount(),
                    request.getOpenid());

            RequestBody requestBody = RequestBody.create(jsonPayload, JSON_TYPE);
            Request httpRequest = new Request.Builder().url(properties.getGatewayUrl() + "/v3/transfer/batches").post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization",
                            "WECHATPAY2-SHA256-RSA2048 mchid=\"" + properties.getMchId() + "\",nonce_str=\"" + nonceStr + "\",timestamp=\""
                                    + timeStamp + "\",serial_no=\"" + properties.getMchSerialNo() + "\",signature=\"" + signature + "\"")
                    .build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                String responseJson = responseBody != null ? responseBody.string() : "";
                log.info("WeChat Pay transfer response: {}", responseJson);

                if (!response.isSuccessful()) {
                    log.error("WeChat Pay transfer HTTP error: {}", response.code());
                    return buildErrorResponse("HTTP_ERROR", "Transfer HTTP request failed");
                }

                return gson.fromJson(responseJson, WeChatPayResponse.class);
            }

        } catch (Exception e) {
            log.error("WeChat Pay transfer exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    private Map<String, Object> buildTransferRequestBody(WeChatPayTransferRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("appid", properties.getAppId());
        body.put("mchid", properties.getMchId());
        body.put("out_batch_no", "BATCH" + request.getOutTransferNo());

        Map<String, Object> transfer = new HashMap<>();
        transfer.put("out_transfer_no", request.getOutTransferNo());
        transfer.put("transfer_scene_id", 1000);
        transfer.put("openid", request.getOpenid());
        transfer.put("name", request.getUserName() != null ? request.getUserName() : "");
        int totalFen = request.getAmount().multiply(new BigDecimal("100")).intValue();
        transfer.put("transfer_amount", totalFen);
        transfer.put("transfer_remark", request.getDescription() != null ? request.getDescription() : "MifosX Withdrawal");

        java.util.List<Map<String, Object>> transfers = java.util.List.of(transfer);
        body.put("transfers", transfers);

        return body;
    }

    private String generateTransferSignature(Map<String, Object> body, String nonceStr, String timeStamp) {
        try {
            String message = properties.getMchId() + "\n" + "POST\n" + "/v3/transfer/batches\n" + timeStamp + "\n" + nonceStr + "\n"
                    + gson.toJson(body) + "\n";

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(message.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("WeChat Pay transfer signature generation failed", e);
        }
    }

    private WeChatPayResponse buildErrorResponse(String code, String msg) {
        WeChatPayResponse response = new WeChatPayResponse();
        response.setCode(code);
        response.setMessage(msg);
        return response;
    }
}
