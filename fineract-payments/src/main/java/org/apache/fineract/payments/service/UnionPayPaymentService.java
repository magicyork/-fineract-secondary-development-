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
package org.apache.fineract.payments.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.payments.config.UnionPayProperties;
import org.apache.fineract.payments.data.UnionPayRequest;
import org.apache.fineract.payments.data.UnionPayResponse;
import org.apache.fineract.payments.gateway.PaymentGateway;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnionPayPaymentService {

    private final PaymentGateway paymentGateway;
    private final UnionPayProperties properties;
    private final SavingsAccountWritePlatformService savingsWritePlatformService;

    @Transactional
    public String deposit(Long savingsId, BigDecimal amount, String cardNo, String cvv2, String expiryDate, String cardHolder,
            String note) {

        log.info("Starting UnionPay deposit: savingsId={}, amount={}, cardNo={}", savingsId, amount, maskCardNo(cardNo));

        String orderId = "DEP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        UnionPayRequest request = UnionPayRequest.builder().merchantId(properties.getMerchantId()).orderId(orderId).amount(amount)
                .cardNo(cardNo).cvv2(cvv2).expiryDate(expiryDate).cardHolderName(cardHolder).transType("DEPOSIT")
                .notifyUrl(properties.getNotifyUrl()).build();

        UnionPayResponse response = paymentGateway.execute(request);

        if (!response.isSuccess()) {
            log.error("UnionPay debit failed: respCode={}, msg={}", response.getRespCode(), response.getRespMsg());
            throw new PlatformServiceUnavailableException("unionpay.payment.failed", "UnionPay debit failed: " + response.getRespMsg());
        }

        log.info("UnionPay debit success: orderId={}, queryId={}", orderId, response.getQueryId());

        LocalDate transactionDate = LocalDate.now(ZoneId.systemDefault());
        Long unionPayTypeId = 100L;

        CommandProcessingResult result = savingsWritePlatformService.depositViaGateway(savingsId, transactionDate, amount, unionPayTypeId,
                maskCardNo(cardNo), null, response.getQueryId(), note);

        log.info("MifosX accounting done: savingsId={}, transactionId={}", savingsId, result.getResourceId());

        return orderId;
    }

    @Transactional
    public void handleCallback(String callbackData) {
        log.info("Received UnionPay callback: {}", callbackData);
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + "********" + cardNo.substring(cardNo.length() - 4);
    }
}
