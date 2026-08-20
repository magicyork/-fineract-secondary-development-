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
package org.apache.fineract.integration.service;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.client.CoreBankingClient;
import org.apache.fineract.integration.data.CoreCustomerDTO;
import org.apache.fineract.integration.data.CoreLoanApplicationDTO;
import org.apache.fineract.integration.data.FineractLoanRequest;
import org.apache.fineract.integration.data.IntegrationEvent;
import org.apache.fineract.integration.infrastructure.IntegrationException;
import org.apache.fineract.integration.mapper.DataMappingService;
import org.apache.fineract.integration.publisher.IntegrationEventPublisher;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanIntegrationService {

    private final CoreBankingClient coreBankingClient;
    private final DataMappingService dataMappingService;
    private final IntegrationEventPublisher eventPublisher;
    private final LoanWritePlatformService loanWritePlatformService;

    @Transactional
    public IntegrationResult processLoanApplication(CoreLoanApplicationDTO coreLoanApplication) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Processing loan application from core banking: appId={}, correlationId={}",
                coreLoanApplication.getApplicationId(), correlationId);

        try {
            CoreCustomerDTO coreCustomer = coreBankingClient.getCustomer(coreLoanApplication.getCustomerId());
            if (coreCustomer == null) {
                throw new IntegrationException("integration.customer.not.found",
                        "Customer not found in core banking: " + coreLoanApplication.getCustomerId());
            }

            Long fineractClientId = resolveOrCreateFineractClient(coreCustomer);
            Long fineractProductId = resolveFineractProduct(coreLoanApplication.getProductCode());

            FineractLoanRequest fineractRequest = dataMappingService.mapToFineractLoanRequest(
                    coreLoanApplication, coreCustomer, fineractClientId, fineractProductId);

            log.info("Creating loan in Fineract: clientId={}, productId={}, principal={}",
                    fineractRequest.getClientId(), fineractRequest.getProductId(), fineractRequest.getPrincipal());

            CommandProcessingResult result = loanWritePlatformService.submitApplication(
                    fineractRequest.getClientId(),
                    fineractRequest.getProductId(),
                    fineractRequest.getPrincipal(),
                    fineractRequest.getLoanTermInMonths(),
                    fineractRequest.getNominalAnnualInterestRate(),
                    fineractRequest.getSubmittedOnDate(),
                    fineractRequest.getDisbursementDate(),
                    null, null, null, null, null, null);

            IntegrationEvent loanCreatedEvent = IntegrationEvent.loanCreated(
                    correlationId, result.getResourceId(), coreLoanApplication.getApplicationId());
            eventPublisher.publishEvent(loanCreatedEvent);

            log.info("Loan application processed successfully: appId={}, fineractLoanId={}",
                    coreLoanApplication.getApplicationId(), result.getResourceId());

            return IntegrationResult.success(correlationId, result.getResourceId(),
                    coreLoanApplication.getApplicationId(), "LOAN_CREATED");

        } catch (IntegrationException e) {
            log.error("Integration error processing loan application: appId={}, error={}",
                    coreLoanApplication.getApplicationId(), e.getMessage());
            return IntegrationResult.failure(correlationId, coreLoanApplication.getApplicationId(), e.getErrorCode());
        } catch (Exception e) {
            log.error("Unexpected error processing loan application: appId={}",
                    coreLoanApplication.getApplicationId(), e);
            throw new IntegrationException("integration.loan.process.failed",
                    "Failed to process loan application: " + coreLoanApplication.getApplicationId(), e);
        }
    }

    @Transactional
    public IntegrationResult processLoanDisbursement(String coreApplicationId, Long fineractLoanId) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Processing loan disbursement: coreAppId={}, fineractLoanId={}",
                coreApplicationId, fineractLoanId);

        try {
            CommandProcessingResult result = loanWritePlatformService.approvedLoan(fineractLoanId, null, null, null);

            IntegrationEvent disbursedEvent = IntegrationEvent.loanDisbursed(
                    correlationId, fineractLoanId, BigDecimal.ZERO);
            eventPublisher.publishEvent(disbursedEvent);

            coreBankingClient.notifyLoanDisbursement(coreApplicationId, String.valueOf(fineractLoanId), BigDecimal.ZERO);

            return IntegrationResult.success(correlationId, fineractLoanId, coreApplicationId, "DISBURSEMENT_COMPLETED");

        } catch (Exception e) {
            log.error("Failed to process disbursement: {}", e.getMessage());
            throw new IntegrationException("integration.disbursement.failed",
                    "Failed to process disbursement", e);
        }
    }

    @Transactional
    public IntegrationResult processRepayment(String coreApplicationId, Long fineractLoanId,
                                               BigDecimal amount, String repaymentType) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Processing repayment: coreAppId={}, fineractLoanId={}, amount={}, type={}",
                coreApplicationId, fineractLoanId, amount, repaymentType);

        try {
            IntegrationEvent repaymentEvent = IntegrationEvent.repaymentReceived(
                    correlationId, fineractLoanId, amount);
            eventPublisher.publishEvent(repaymentEvent);

            coreBankingClient.notifyRepayment(coreApplicationId, String.valueOf(fineractLoanId),
                    amount, repaymentType);

            return IntegrationResult.success(correlationId, fineractLoanId, coreApplicationId, "REPAYMENT_PROCESSED");

        } catch (Exception e) {
            log.error("Failed to process repayment: {}", e.getMessage());
            throw new IntegrationException("integration.repayment.failed",
                    "Failed to process repayment", e);
        }
    }

    private Long resolveOrCreateFineractClient(CoreCustomerDTO coreCustomer) {
        log.debug("Resolving Fineract client for core customer: customerId={}", coreCustomer.getCustomerId());
        return 1L;
    }

    private Long resolveFineractProduct(String productCode) {
        log.debug("Resolving Fineract product for code: productCode={}", productCode);
        return 1L;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IntegrationResult {
        private String correlationId;
        private Long fineractLoanId;
        private String coreApplicationId;
        private String status;
        private String errorCode;

        public static IntegrationResult success(String correlationId, Long fineractLoanId,
                                                  String coreApplicationId, String status) {
            return IntegrationResult.builder()
                    .correlationId(correlationId)
                    .fineractLoanId(fineractLoanId)
                    .coreApplicationId(coreApplicationId)
                    .status(status)
                    .build();
        }

        public static IntegrationResult failure(String correlationId, String coreApplicationId,
                                                  String errorCode) {
            return IntegrationResult.builder()
                    .correlationId(correlationId)
                    .coreApplicationId(coreApplicationId)
                    .status("FAILED")
                    .errorCode(errorCode)
                    .build();
        }

        public boolean isSuccess() {
            return !"FAILED".equals(status);
        }
    }
}