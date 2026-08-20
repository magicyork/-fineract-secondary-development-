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

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.payments.service.UnionPayPaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/unionpay")
@RequiredArgsConstructor
public class UnionPayCallbackApiResource {

    private final UnionPayPaymentService paymentService;

    @PostMapping("/callback")
    public Map<String, String> callback(@RequestBody String callbackData) {
        log.info("UnionPay callback notification: {}", callbackData);

        try {
            paymentService.handleCallback(callbackData);

            Map<String, String> response = new HashMap<>();
            response.put("respCode", "000000");
            response.put("respMsg", "success");
            return response;

        } catch (Exception e) {
            log.error("UnionPay callback processing failed", e);
            Map<String, String> response = new HashMap<>();
            response.put("respCode", "999999");
            response.put("respMsg", "fail");
            return response;
        }
    }

    @GetMapping("/confirm")
    public Map<String, String> confirm(@RequestParam String orderId) {
        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", "PROCESSING");
        return response;
    }
}
