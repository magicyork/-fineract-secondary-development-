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
import org.apache.fineract.payments.service.UnionPayPaymentService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/savingsaccounts")
@RequiredArgsConstructor
public class UnionPayDepositApiResource {

    private final UnionPayPaymentService unionPayPaymentService;

    @PostMapping("/{savingsId}/unionpay-deposit")
    public Map<String, Object> unionPayDeposit(@PathVariable Long savingsId, @RequestBody Map<String, Object> request) {

        log.info("UnionPay deposit request: savingsId={}", savingsId);

        BigDecimal amount = new BigDecimal((String) request.get("transactionAmount"));
        String cardNo = (String) request.get("accountNumber");
        String cvv2 = (String) request.get("cvv2");
        String expiryDate = (String) request.get("expiryDate");
        String cardHolder = (String) request.get("cardHolderName");
        String note = (String) request.get("note");

        String orderId = unionPayPaymentService.deposit(savingsId, amount, cardNo, cvv2, expiryDate, cardHolder, note);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("savingsId", savingsId);
        response.put("transactionAmount", amount);
        response.put("status", "SUCCESS");

        return response;
    }
}
