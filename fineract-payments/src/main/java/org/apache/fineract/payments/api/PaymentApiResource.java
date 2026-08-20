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
package org.apache.fineract.payments.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.payments.PaymentConstants;
import org.apache.fineract.payments.data.PaymentDepositRequest;
import org.apache.fineract.payments.data.PaymentDepositResult;
import org.apache.fineract.payments.data.PaymentTransferRequest;
import org.apache.fineract.payments.data.PaymentTransferResult;
import org.apache.fineract.payments.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/savingsaccounts")
@RequiredArgsConstructor
public class PaymentApiResource {

    private final PaymentService paymentService;

    @PostMapping("/{savingsId}/payment-deposit")
    public Map<String, Object> paymentDeposit(@PathVariable Long savingsId, @RequestBody Map<String, Object> request) {

        String paymentMethod = (String) request.get("paymentMethod");
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required: unionpay, alipay, or wechatpay");
        }

        BigDecimal amount = new BigDecimal((String) request.get("transactionAmount"));

        PaymentDepositRequest depositRequest = PaymentDepositRequest.builder().paymentMethod(paymentMethod).savingsId(savingsId)
                .transactionAmount(amount).accountNumber((String) request.get("accountNumber")).cvv2((String) request.get("cvv2"))
                .expiryDate((String) request.get("expiryDate")).cardHolderName((String) request.get("cardHolderName"))
                .openid((String) request.get("openid")).authToken((String) request.get("authToken"))
                .buyerId((String) request.get("buyerId")).subject((String) request.get("subject"))
                .description((String) request.get("description")).note((String) request.get("note")).build();

        PaymentDepositResult result = paymentService.deposit(depositRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", result.getOrderId());
        response.put("savingsId", result.getSavingsId());
        response.put("paymentMethod", result.getPaymentMethod());
        response.put("status", result.getStatus());
        response.put("externalTransactionId", result.getExternalTransactionId());

        return response;
    }

    @GetMapping("/{savingsId}/payment-methods")
    public Map<String, Object> getPaymentMethods(@PathVariable Long savingsId) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> unionpay = new HashMap<>();
        unionpay.put("name", PaymentConstants.METHOD_UNIONPAY);
        unionpay.put("enabled", paymentService.isMethodEnabled(PaymentConstants.METHOD_UNIONPAY));
        unionpay.put("typeId", PaymentConstants.TYPE_ID_UNIONPAY);

        Map<String, Object> alipay = new HashMap<>();
        alipay.put("name", PaymentConstants.METHOD_ALIPAY);
        alipay.put("enabled", paymentService.isMethodEnabled(PaymentConstants.METHOD_ALIPAY));
        alipay.put("typeId", PaymentConstants.TYPE_ID_ALIPAY);

        Map<String, Object> wechatpay = new HashMap<>();
        wechatpay.put("name", PaymentConstants.METHOD_WECHATPAY);
        wechatpay.put("enabled", paymentService.isMethodEnabled(PaymentConstants.METHOD_WECHATPAY));
        wechatpay.put("typeId", PaymentConstants.TYPE_ID_WECHATPAY);

        response.put("savingsId", savingsId);
        response.put("paymentMethods", java.util.List.of(unionpay, alipay, wechatpay));

        return response;
    }

    @PostMapping("/{savingsId}/payment-withdraw")
    public Map<String, Object> paymentWithdraw(@PathVariable Long savingsId, @RequestBody Map<String, Object> request) {

        String paymentMethod = (String) request.get("paymentMethod");
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required: unionpay, alipay, or wechatpay");
        }

        BigDecimal amount = new BigDecimal((String) request.get("transactionAmount"));

        PaymentTransferRequest transferRequest = PaymentTransferRequest.builder().paymentMethod(paymentMethod).savingsId(savingsId)
                .transactionAmount(amount).accountNumber((String) request.get("accountNumber")).cardNo((String) request.get("cardNo"))
                .cvv2((String) request.get("cvv2")).expiryDate((String) request.get("expiryDate"))
                .cardHolderName((String) request.get("cardHolderName")).openid((String) request.get("openid"))
                .payeeAccount((String) request.get("payeeAccount")).payeeType((String) request.get("payeeType"))
                .payeeName((String) request.get("payeeName")).subject((String) request.get("subject"))
                .description((String) request.get("description")).note((String) request.get("note")).build();

        PaymentTransferResult result = paymentService.withdraw(transferRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", result.getOrderId());
        response.put("savingsId", result.getSavingsId());
        response.put("paymentMethod", result.getPaymentMethod());
        response.put("status", result.getStatus());
        response.put("externalTransactionId", result.getExternalTransactionId());

        return response;
    }
}
