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
package org.apache.fineract.integration.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.config.CoreBankingProperties;
import org.apache.fineract.integration.data.CoreLoanApplicationDTO;
import org.apache.fineract.integration.service.LoanIntegrationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreBankingEventConsumer {

    private final CoreBankingProperties properties;
    private final ObjectMapper objectMapper;
    private final LoanIntegrationService loanIntegrationService;

    @KafkaListener(
            topics = "${fineract.integration.core.event.coreEventTopic:core-banking-events}",
            groupId = "${fineract.integration.core.event.consumerGroup:fineract-integration}"
    )
    public void consumeCoreBankingEvent(String message, String key) {
        log.info("Received core banking event: key={}, message={}", key, message);

        try {
            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.path("eventType").asText();

            switch (eventType) {
                case "LOAN_APPLICATION_SUBMITTED" -> handleLoanApplication(eventNode);
                case "LOAN_APPLICATION_APPROVED" -> handleLoanApproved(eventNode);
                case "LOAN_APPLICATION_REJECTED" -> handleLoanRejected(eventNode);
                case "CUSTOMER_KYC_UPDATED" -> handleCustomerKycUpdated(eventNode);
                case "ACCOUNT_CREATED" -> handleAccountCreated(eventNode);
                case "PRODUCT_UPDATED" -> handleProductUpdated(eventNode);
                default -> log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to consume core banking event: key={}, error={}", key, e.getMessage());
        }
    }

    private void handleLoanApplication(JsonNode eventNode) {
        log.info("Handling loan application event");
        try {
            CoreLoanApplicationDTO loanApp = parseLoanApplication(eventNode.path("payload"));
            LoanIntegrationService.IntegrationResult result = loanIntegrationService.processLoanApplication(loanApp);

            if (result.isSuccess()) {
                log.info("Loan application processed: coreAppId={}, fineractLoanId={}",
                        result.getCoreApplicationId(), result.getFineractLoanId());
            } else {
                log.warn("Loan application failed: coreAppId={}, errorCode={}",
                        result.getCoreApplicationId(), result.getErrorCode());
            }
        } catch (Exception e) {
            log.error("Failed to handle loan application: {}", e.getMessage());
        }
    }

    private void handleLoanApproved(JsonNode eventNode) {
        log.info("Handling loan approval event");
        String coreAppId = eventNode.path("payload").path("applicationId").asText();
        log.info("Loan approved in core: coreAppId={}", coreAppId);
    }

    private void handleLoanRejected(JsonNode eventNode) {
        log.info("Handling loan rejection event");
        String coreAppId = eventNode.path("payload").path("applicationId").asText();
        String reason = eventNode.path("payload").path("rejectionReason").asText();
        log.info("Loan rejected in core: coreAppId={}, reason={}", coreAppId, reason);
    }

    private void handleCustomerKycUpdated(JsonNode eventNode) {
        log.info("Handling KYC update event");
        String customerId = eventNode.path("payload").path("customerId").asText();
        log.info("KYC updated for customer: customerId={}", customerId);
    }

    private void handleAccountCreated(JsonNode eventNode) {
        log.info("Handling account created event");
        String accountNo = eventNode.path("payload").path("accountNo").asText();
        String customerId = eventNode.path("payload").path("customerId").asText();
        log.info("Account created: accountNo={}, customerId={}", accountNo, customerId);
    }

    private void handleProductUpdated(JsonNode eventNode) {
        log.info("Handling product updated event");
        String productCode = eventNode.path("payload").path("productCode").asText();
        log.info("Product updated: productCode={}", productCode);
    }

    private CoreLoanApplicationDTO parseLoanApplication(JsonNode payload) {
        CoreLoanApplicationDTO.CoreLoanApplicationDTOBuilder builder = CoreLoanApplicationDTO.builder()
                .applicationId(payload.path("applicationId").asText())
                .customerId(payload.path("customerId").asText())
                .productCode(payload.path("productCode").asText())
                .loanAmount(payload.path("loanAmount").asDecimal())
                .repaymentPeriod(payload.path("repaymentPeriod").asInt())
                .branchCode(payload.path("branchCode").asText())
                .officerCode(payload.path("officerCode").asText());

        if (payload.has("applicationDate")) {
            builder.applicationDate(java.time.LocalDate.parse(payload.path("applicationDate").asText()));
        }
        if (payload.has("expectedDisbursementDate")) {
            builder.expectedDisbursementDate(java.time.LocalDate.parse(payload.path("expectedDisbursementDate").asText()));
        }
        if (payload.has("collateralType")) {
            builder.collateralType(payload.path("collateralType").asText());
        }
        if (payload.has("collateralValue")) {
            builder.collateralValue(payload.path("collateralValue").asDecimal());
        }

        return builder.build();
    }
}