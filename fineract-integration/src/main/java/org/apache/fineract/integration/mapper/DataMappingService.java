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
package org.apache.fineract.integration.mapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.data.CoreCustomerDTO;
import org.apache.fineract.integration.data.CoreLoanApplicationDTO;
import org.apache.fineract.integration.data.FineractLoanRequest;
import org.apache.fineract.integration.infrastructure.IntegrationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataMappingService {

    public FineractLoanRequest mapToFineractLoanRequest(CoreLoanApplicationDTO coreLoan,
                                                        CoreCustomerDTO coreCustomer,
                                                        Long fineractClientId,
                                                        Long fineractProductId) {
        log.debug("Mapping core loan application to Fineract request: appId={}", coreLoan.getApplicationId());

        FineractLoanRequest.FineractLoanRequestBuilder builder = FineractLoanRequest.builder()
                .clientId(fineractClientId)
                .productId(fineractProductId)
                .principal(coreLoan.getLoanAmount())
                .loanTermInMonths(coreLoan.getRepaymentPeriod())
                .disbursementDate(coreLoan.getExpectedDisbursementDate() != null
                        ? coreLoan.getExpectedDisbursementDate()
                        : coreLoan.getApplicationDate())
                .submittedOnDate(coreLoan.getApplicationDate())
                .externalId("CORE-" + coreLoan.getApplicationId());

        mapInterestRate(builder, coreCustomer);
        mapCollateral(builder, coreLoan);
        mapGuarantor(builder, coreLoan);

        return builder.build();
    }

    public CoreLoanApplicationDTO mapToCoreLoanApplication(FineractLoanRequest fineractRequest,
                                                            Long fineractLoanId) {
        return CoreLoanApplicationDTO.builder()
                .applicationId(fineractRequest.getExternalId())
                .customerId(String.valueOf(fineractRequest.getClientId()))
                .loanAmount(fineractRequest.getPrincipal())
                .applicationDate(fineractRequest.getSubmittedOnDate())
                .expectedDisbursementDate(fineractRequest.getDisbursementDate())
                .build();
    }

    public String mapCoreCustomerType(String coreCustomerType) {
        return switch (coreCustomerType.toUpperCase()) {
            case "RETAIL", "个人", "RESIDENT" -> "INDIVIDUAL";
            case "CORPORATE", "对公", "ENTERPRISE" -> "GROUP";
            case "SME", "小微" -> "INDIVIDUAL";
            default -> throw new IntegrationException("mapping.customerType.unknown",
                    "Unknown customer type: " + coreCustomerType);
        };
    }

    public String mapCoreLoanStatus(String coreStatus) {
        return switch (coreStatus.toUpperCase()) {
            case "APPROVED", "已审批" -> "APPROVED";
            case "PENDING", "待审批" -> "SUBMITTED_AND_PENDING_APPROVAL";
            case "REJECTED", "已拒绝" -> "REJECTED";
            case "DISBURSED", "已发放" -> "DISBURSED";
            case "CLOSED", "已结清" -> "CLOSED";
            default -> throw new IntegrationException("mapping.loanStatus.unknown",
                    "Unknown loan status: " + coreStatus);
        };
    }

    private void mapInterestRate(FineractLoanRequest.FineractLoanRequestBuilder builder,
                                 CoreCustomerDTO coreCustomer) {
        if (coreCustomer != null && coreCustomer.getCreditRating() != null) {
            builder.nominalAnnualInterestRate(determineInterestRate(coreCustomer.getCreditRating()));
        }
    }

    private java.math.BigDecimal determineInterestRate(String creditRating) {
        return switch (creditRating.toUpperCase()) {
            case "AAA", "AA" -> java.math.BigDecimal.valueOf(4.35);
            case "A", "BBB" -> java.math.BigDecimal.valueOf(5.25);
            case "BB", "B" -> java.math.BigDecimal.valueOf(6.75);
            case "CCC", "CC", "C" -> java.math.BigDecimal.valueOf(8.50);
            default -> java.math.BigDecimal.valueOf(5.00);
        };
    }

    private void mapCollateral(FineractLoanRequest.FineractLoanRequestBuilder builder,
                               CoreLoanApplicationDTO coreLoan) {
        if (coreLoan.getCollateralType() != null) {
            FineractLoanRequest.CollateralPayload collateral = FineractLoanRequest.CollateralPayload.builder()
                    .collateralTypeId(mapCollateralType(coreLoan.getCollateralType()))
                    .collateralValue(String.valueOf(coreLoan.getCollateralValue() != null
                            ? coreLoan.getCollateralValue()
                            : 0))
                    .collateralQuality("GOOD")
                    .build();
            builder.collateral(collateral);
        }
    }

    private Long mapCollateralType(String coreType) {
        return switch (coreType.toUpperCase()) {
            case "REAL_ESTATE", "房产" -> 1L;
            case "VEHICLE", "车辆" -> 2L;
            case "DEPOSIT", "存单" -> 3L;
            case "STOCK", "股票" -> 4L;
            case "GUARANTEE", "保证" -> 5L;
            default -> 1L;
        };
    }

    private void mapGuarantor(FineractLoanRequest.FineractLoanRequestBuilder builder,
                              CoreLoanApplicationDTO coreLoan) {
        if (coreLoan.getGuarantorId() != null) {
            log.debug("Guarantor mapping would be done separately for guarantorId={}", coreLoan.getGuarantorId());
        }
    }
}