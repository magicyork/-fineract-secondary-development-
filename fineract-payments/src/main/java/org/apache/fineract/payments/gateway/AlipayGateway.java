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
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.fineract.payments.config.AlipayProperties;
import org.apache.fineract.payments.data.AlipayRequest;
import org.apache.fineract.payments.data.AlipayResponse;
import org.apache.fineract.payments.data.AlipayTransferRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayGateway {

    private final OkHttpClient okHttpClient;
    private final AlipayProperties properties;
    private final Gson gson = new GsonBuilder().create();

    public AlipayResponse execute(AlipayRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Alipay payment is not enabled");
        }

        try {
            Map<String, String> params = buildRequestParams(request);
            String sign = generateSignature(params);
            params.put("sign", sign);

            log.info("Alipay request: outTradeNo={}, totalAmount={}", request.getOutTradeNo(), request.getTotalAmount());

            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
            RequestBody body = formBuilder.build();

            Request httpRequest = new Request.Builder().url(properties.getGatewayUrl()).post(body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded").build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                String responseJson = responseBody != null ? responseBody.string() : "";
                log.info("Alipay response: {}", responseJson);

                if (!response.isSuccessful()) {
                    log.error("Alipay HTTP error: {}", response.code());
                    return buildErrorResponse("HTTP_ERROR", "HTTP request failed");
                }

                Map<String, String> responseMap = gson.fromJson(responseJson, new TypeToken<Map<String, String>>(){}.getType());
                String alipayResponseJson = responseMap.getOrDefault("alipay_trade_app_pay_response", responseJson);
                AlipayResponse alipayResponse = gson.fromJson(alipayResponseJson, AlipayResponse.class);

                verifySignature(responseMap, alipayResponseJson);

                return alipayResponse;
            }

        } catch (Exception e) {
            log.error("Alipay payment exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    public String getType() {
        return "ALIPAY";
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private Map<String, String> buildRequestParams(AlipayRequest request) {
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", properties.getAppId());
        params.put("method", "alipay.trade.app.pay");
        params.put("charset", properties.getCharset());
        params.put("sign_type", properties.getSignType());
        params.put("out_trade_no", request.getOutTradeNo());
        params.put("total_amount", request.getTotalAmount().toString());
        params.put("subject", request.getSubject());
        params.put("product_code", "QUICK_MSECURITY_PAY");
        params.put("notify_url", properties.getNotifyUrl());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("version", "1.0");

        if (request.getAuthToken() != null) {
            params.put("auth_token", request.getAuthToken());
        }
        if (request.getBuyerId() != null) {
            params.put("buyer_id", request.getBuyerId());
        }

        return params;
    }

    private String generateSignature(Map<String, String> params) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null && !"sign".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            sb.append(properties.getPrivateKey());

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString().toUpperCase(Locale.ROOT);

        } catch (Exception e) {
            throw new RuntimeException("Alipay signature generation failed", e);
        }
    }

    public AlipayResponse transfer(AlipayTransferRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Alipay is not enabled");
        }

        try {
            Map<String, String> params = buildTransferParams(request);
            String sign = generateSignature(params);
            params.put("sign", sign);

            log.info("Alipay transfer request: outBizNo={}, amount={}, payeeAccount={}", request.getOutBizNo(), request.getTransAmount(),
                    maskAccount(request.getPayeeAccount()));

            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
            RequestBody body = formBuilder.build();

            Request httpRequest = new Request.Builder().url(properties.getGatewayUrl()).post(body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded").build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                String responseJson = responseBody != null ? responseBody.string() : "";
                log.info("Alipay transfer response: {}", responseJson);

                if (!response.isSuccessful()) {
                    log.error("Alipay transfer HTTP error: {}", response.code());
                    return buildErrorResponse("HTTP_ERROR", "Transfer HTTP request failed");
                }

                Map<String, String> responseMap = gson.fromJson(responseJson, new TypeToken<Map<String, String>>(){}.getType());
                String alipayResponseJson = responseMap.getOrDefault("alipay_fund_trans_uni_transfer_response", responseJson);
                AlipayResponse alipayResponse = gson.fromJson(alipayResponseJson, AlipayResponse.class);

                return alipayResponse;
            }

        } catch (Exception e) {
            log.error("Alipay transfer exception", e);
            return buildErrorResponse("SYSTEM_ERROR", e.getMessage());
        }
    }

    private Map<String, String> buildTransferParams(AlipayTransferRequest request) {
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", properties.getAppId());
        params.put("method", "alipay.fund.trans.uni.transfer");
        params.put("charset", properties.getCharset());
        params.put("sign_type", properties.getSignType());
        params.put("out_biz_no", request.getOutBizNo());
        params.put("trans_amount", request.getTransAmount().toString());
        params.put("product_code", request.getProductCode() != null ? request.getProductCode() : "TRANS_ACCOUNT_NO_PWD");
        params.put("biz_product_code", request.getBizProductCode() != null ? request.getBizProductCode() : "DIRECT_TRANSFER");
        params.put("title", request.getTitle() != null ? request.getTitle() : "MifosX Withdrawal");
        params.put("notify_url", properties.getNotifyUrl());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("version", "1.0");

        if (request.getPayeeAccount() != null) {
            params.put("payee_account", request.getPayeeAccount());
        }
        if (request.getPayeeType() != null) {
            params.put("payee_type", request.getPayeeType());
        }
        if (request.getPayeeName() != null) {
            params.put("payee_name", request.getPayeeName());
        }
        if (request.getOriginalOrderId() != null) {
            params.put("original_order_id", request.getOriginalOrderId());
        }

        return params;
    }

    private String maskAccount(String account) {
        if (account == null || account.length() < 6) {
            return "****";
        }
        return account.substring(0, 3) + "****" + account.substring(account.length() - 3);
    }

    private void verifySignature(Map<String, String> responseMap, String responseJson) {
        log.debug("Alipay response signature verification would happen here");
    }

    private AlipayResponse buildErrorResponse(String code, String msg) {
        AlipayResponse response = new AlipayResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
