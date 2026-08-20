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
import org.apache.fineract.payments.PaymentConstants;
import org.apache.fineract.payments.config.AlipayProperties;
import org.apache.fineract.payments.config.UnionPayProperties;
import org.apache.fineract.payments.config.WeChatPayProperties;
import org.apache.fineract.payments.data.AlipayRequest;
import org.apache.fineract.payments.data.AlipayResponse;
import org.apache.fineract.payments.data.AlipayTransferRequest;
import org.apache.fineract.payments.data.PaymentDepositRequest;
import org.apache.fineract.payments.data.PaymentDepositResult;
import org.apache.fineract.payments.data.PaymentTransferRequest;
import org.apache.fineract.payments.data.PaymentTransferResult;
import org.apache.fineract.payments.data.UnionPayRequest;
import org.apache.fineract.payments.data.UnionPayResponse;
import org.apache.fineract.payments.data.UnionPayTransferRequest;
import org.apache.fineract.payments.data.WeChatPayRequest;
import org.apache.fineract.payments.data.WeChatPayResponse;
import org.apache.fineract.payments.data.WeChatPayTransferRequest;
import org.apache.fineract.payments.gateway.PaymentGatewayFactory;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGatewayFactory gatewayFactory;
    private final UnionPayProperties unionPayProperties;
    private final AlipayProperties alipayProperties;
    private final WeChatPayProperties weChatPayProperties;
    private final SavingsAccountWritePlatformService savingsWritePlatformService;

    @Transactional
    public PaymentDepositResult deposit(PaymentDepositRequest request) {
        String method = request.getPaymentMethod().toLowerCase(Locale.ROOT);
        log.info("Starting {} deposit: savingsId={}, amount={}", method, request.getSavingsId(), request.getTransactionAmount());

        return switch (method) {
            case PaymentConstants.METHOD_UNIONPAY -> processUnionPayDeposit(request);
            case PaymentConstants.METHOD_ALIPAY -> processAlipayDeposit(request);
            case PaymentConstants.METHOD_WECHATPAY -> processWeChatPayDeposit(request);
            default -> throw new IllegalArgumentException("Unsupported payment method: " + method);
        };
    }

    private PaymentDepositResult processUnionPayDeposit(PaymentDepositRequest request) {
        String orderId = "DEP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        UnionPayRequest upRequest = UnionPayRequest.builder().merchantId(unionPayProperties.getMerchantId()).orderId(orderId)
                .amount(request.getTransactionAmount()).cardNo(request.getAccountNumber()).cvv2(request.getCvv2())
                .expiryDate(request.getExpiryDate()).cardHolderName(request.getCardHolderName()).transType("DEPOSIT")
                .notifyUrl(unionPayProperties.getNotifyUrl()).build();

        UnionPayResponse response = gatewayFactory.executeUnionPay(upRequest);

        if (!response.isSuccess()) {
            log.error("UnionPay debit failed: {}", response.getRespMsg());
            throw new PlatformServiceUnavailableException("unionpay.payment.failed", response.getRespMsg());
        }

        doAccounting(request.getSavingsId(), request.getTransactionAmount(),
                PaymentConstants.TYPE_ID_UNIONPAY, maskCardNo(request.getAccountNumber()), response.getQueryId(), request.getNote());

        return PaymentDepositResult.builder().orderId(orderId).savingsId(request.getSavingsId())
                .paymentMethod(PaymentConstants.METHOD_UNIONPAY).paymentMethodId(String.valueOf(PaymentConstants.TYPE_ID_UNIONPAY))
                .externalTransactionId(response.getQueryId()).status("SUCCESS").build();
    }

    private PaymentDepositResult processAlipayDeposit(PaymentDepositRequest request) {
        String orderId = "DEP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        AlipayRequest alipayRequest = AlipayRequest.builder().appId(alipayProperties.getAppId()).outTradeNo(orderId)
                .totalAmount(request.getTransactionAmount()).subject(request.getSubject() != null ? request.getSubject() : "MifosX Deposit")
                .productCode("QUICK_MSECURITY_PAY").authToken(request.getAuthToken()).buyerId(request.getBuyerId())
                .notifyUrl(alipayProperties.getNotifyUrl()).build();

        AlipayResponse response = gatewayFactory.executeAlipay(alipayRequest);

        if (!response.isSuccess()) {
            log.error("Alipay debit failed: {}", response.getMsg());
            throw new PlatformServiceUnavailableException("alipay.payment.failed", response.getMsg());
        }

        doAccounting(request.getSavingsId(), request.getTransactionAmount(),
                PaymentConstants.TYPE_ID_ALIPAY, request.getAccountNumber(), response.getTradeNo(), request.getNote());

        return PaymentDepositResult.builder().orderId(orderId).savingsId(request.getSavingsId())
                .paymentMethod(PaymentConstants.METHOD_ALIPAY).paymentMethodId(String.valueOf(PaymentConstants.TYPE_ID_ALIPAY))
                .externalTransactionId(response.getTradeNo()).status("SUCCESS").build();
    }

    private PaymentDepositResult processWeChatPayDeposit(PaymentDepositRequest request) {
        String orderId = "DEP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        WeChatPayRequest wxRequest = WeChatPayRequest.builder().appId(weChatPayProperties.getAppId()).mchId(weChatPayProperties.getMchId())
                .outTradeNo(orderId).description(request.getDescription() != null ? request.getDescription() : "MifosX Deposit")
                .amount(request.getTransactionAmount()).openid(request.getOpenid()).notifyUrl(weChatPayProperties.getNotifyUrl()).build();

        WeChatPayResponse response = gatewayFactory.executeWeChatPay(wxRequest);

        if (!response.isSuccess()) {
            log.error("WeChat Pay debit failed: {}", response.getMessage());
            throw new PlatformServiceUnavailableException("wechatpay.payment.failed", response.getMessage());
        }

        doAccounting(request.getSavingsId(), request.getTransactionAmount(),
                PaymentConstants.TYPE_ID_WECHATPAY, request.getAccountNumber(), response.getTransactionId(), request.getNote());

        return PaymentDepositResult.builder().orderId(orderId).savingsId(request.getSavingsId())
                .paymentMethod(PaymentConstants.METHOD_WECHATPAY).paymentMethodId(String.valueOf(PaymentConstants.TYPE_ID_WECHATPAY))
                .externalTransactionId(response.getTransactionId()).status("SUCCESS").build();
    }

    @Transactional
    public PaymentTransferResult withdraw(PaymentTransferRequest request) {
        String method = request.getPaymentMethod().toLowerCase(Locale.ROOT);
        log.info("Starting {} withdrawal: savingsId={}, amount={}", method, request.getSavingsId(), request.getTransactionAmount());

        return switch (method) {
            case PaymentConstants.METHOD_UNIONPAY -> processUnionPayWithdrawal(request);
            case PaymentConstants.METHOD_ALIPAY -> processAlipayWithdrawal(request);
            case PaymentConstants.METHOD_WECHATPAY -> processWeChatPayWithdrawal(request);
            default -> throw new IllegalArgumentException("Unsupported payment method: " + method);
        };
    }

    private PaymentTransferResult processUnionPayWithdrawal(PaymentTransferRequest request) {
        String orderId = "WDR" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        UnionPayTransferRequest transferRequest = UnionPayTransferRequest.builder().merchantId(unionPayProperties.getMerchantId())
                .orderId(orderId).amount(request.getTransactionAmount()).cardNo(request.getCardNo())
                .cardHolderName(request.getCardHolderName()).transType("TRANSFER").notifyUrl(unionPayProperties.getNotifyUrl()).build();

        UnionPayResponse response = gatewayFactory.transferUnionPay(transferRequest);

        if (!response.isSuccess()) {
            log.error("UnionPay transfer failed: {}", response.getRespMsg());
            throw new PlatformServiceUnavailableException("unionpay.transfer.failed", response.getRespMsg());
        }

        doWithdrawalAccounting(request.getSavingsId(), request.getTransactionAmount(),
                PaymentConstants.TYPE_ID_UNIONPAY, maskCardNo(request.getCardNo()), response.getQueryId(), request.getNote());

        return PaymentTransferResult.builder().orderId(orderId).savingsId(request.getSavingsId())
                .paymentMethod(PaymentConstants.METHOD_UNIONPAY).paymentMethodId(String.valueOf(PaymentConstants.TYPE_ID_UNIONPAY))
                .externalTransactionId(response.getQueryId()).status("SUCCESS").build();
    }

    private PaymentTransferResult processAlipayWithdrawal(PaymentTransferRequest request) {
        String orderId = "WDR" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        AlipayTransferRequest transferRequest = AlipayTransferRequest.builder().appId(alipayProperties.getAppId()).outBizNo(orderId)
                .transAmount(request.getTransactionAmount()).payeeAccount(request.getPayeeAccount())
                .payeeType(request.getPayeeType() != null ? request.getPayeeType() : "ALIPAY_LOGON_ID").payeeName(request.getPayeeName())
                .title(request.getSubject() != null ? request.getSubject() : "MifosX Withdrawal").notifyUrl(alipayProperties.getNotifyUrl())
                .build();

        AlipayResponse response = gatewayFactory.transferAlipay(transferRequest);

        if (!response.isSuccess()) {
            log.error("Alipay transfer failed: {}", response.getMsg());
            throw new PlatformServiceUnavailableException("alipay.transfer.failed", response.getMsg());
        }

        doWithdrawalAccounting(request.getSavingsId(), request.getTransactionAmount(),
                PaymentConstants.TYPE_ID_ALIPAY, request.getPayeeAccount(), response.getTradeNo(), request.getNote());

        return PaymentTransferResult.builder().orderId(orderId).savingsId(request.getSavingsId())
                .paymentMethod(PaymentConstants.METHOD_ALIPAY).paymentMethodId(String.valueOf(PaymentConstants.TYPE_ID_ALIPAY))
                .externalTransactionId(response.getTradeNo()).status("SUCCESS").build();
    }

    private PaymentTransferResult processWeChatPayWithdrawal(PaymentTransferRequest request) {
        String orderId = "WDR" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);

        WeChatPayTransferRequest transferRequest = WeChatPayTransferRequest.builder().appId(weChatPayProperties.getAppId())
                .mchId(weChatPayProperties.getMchId()).outTransferNo(orderId).amount(request.getTransactionAmount())
                .openid(request.getOpenid()).userName(request.getPayeeName())
                .description(request.getDescription() != null ? request.getDescription() : "MifosX Withdrawal")
                .notifyUrl(weChatPayProperties.getNotifyUrl()).build();

        WeChatPayResponse response = gatewayFactory.transferWeChatPay(transferRequest);

        if (!response.isSuccess()) {
            log.error("WeChat Pay transfer failed: {}", response.getMessage());
            throw new PlatformServiceUnavailableException("wechatpay.transfer.failed", response.getMessage());
        }

        doWithdrawalAccounting(request.getSavingsId(), request.getTransactionAmount(),
                PaymentConstants.TYPE_ID_WECHATPAY, request.getOpenid(), response.getTransactionId(), request.getNote());

        return PaymentTransferResult.builder().orderId(orderId).savingsId(request.getSavingsId())
                .paymentMethod(PaymentConstants.METHOD_WECHATPAY).paymentMethodId(String.valueOf(PaymentConstants.TYPE_ID_WECHATPAY))
                .externalTransactionId(response.getTransactionId()).status("SUCCESS").build();
    }

    private CommandProcessingResult doWithdrawalAccounting(Long savingsId, BigDecimal amount, Long paymentTypeId, String accountNumber,
            String externalRef, String note) {
        LocalDate transactionDate = LocalDate.now(ZoneId.systemDefault());
        return savingsWritePlatformService.withdrawViaGateway(savingsId, transactionDate, amount, paymentTypeId, accountNumber, null,
                externalRef, note);
    }

    private CommandProcessingResult doAccounting(Long savingsId, BigDecimal amount, Long paymentTypeId, String accountNumber,
            String externalRef, String note) {
        LocalDate transactionDate = LocalDate.now(ZoneId.systemDefault());
        return savingsWritePlatformService.depositViaGateway(savingsId, transactionDate, amount, paymentTypeId, accountNumber, null,
                externalRef, note);
    }

    public boolean isMethodEnabled(String method) {
        return gatewayFactory.isMethodEnabled(method);
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + "********" + cardNo.substring(cardNo.length() - 4);
    }
}
