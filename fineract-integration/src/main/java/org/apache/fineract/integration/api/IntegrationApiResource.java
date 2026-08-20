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
package org.apache.fineract.integration.api;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.data.CoreLoanApplicationDTO;
import org.apache.fineract.integration.service.LoanIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class IntegrationApiResource {

    private final LoanIntegrationService loanIntegrationService;

    @PostMapping("/loans/applications")
    public ResponseEntity<LoanIntegrationService.IntegrationResult> submitLoanApplication(
            @RequestBody CoreLoanApplicationDTO request) {
        log.info("Received loan application from core banking: appId={}", request.getApplicationId());
        LoanIntegrationService.IntegrationResult result = loanIntegrationService.processLoanApplication(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/loans/disbursements/{fineractLoanId}")
    public ResponseEntity<LoanIntegrationService.IntegrationResult> processDisbursement(
            @PathVariable Long fineractLoanId,
            @RequestParam String coreApplicationId) {
        log.info("Processing disbursement: fineractLoanId={}, coreAppId={}", fineractLoanId, coreApplicationId);
        LoanIntegrationService.IntegrationResult result = loanIntegrationService.processLoanDisbursement(
                coreApplicationId, fineractLoanId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/loans/{fineractLoanId}/repayments")
    public ResponseEntity<LoanIntegrationService.IntegrationResult> processRepayment(
            @PathVariable Long fineractLoanId,
            @RequestParam String coreApplicationId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "NORMAL") String repaymentType) {
        log.info("Processing repayment: fineractLoanId={}, amount={}, type={}",
                fineractLoanId, amount, repaymentType);
        LoanIntegrationService.IntegrationResult result = loanIntegrationService.processRepayment(
                coreApplicationId, fineractLoanId, amount, repaymentType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fineract Integration Layer is running");
    }
}