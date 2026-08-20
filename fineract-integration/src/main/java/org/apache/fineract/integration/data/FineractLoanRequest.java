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
package org.apache.fineract.integration.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineractLoanRequest {

    private Long clientId;
    private Long productId;
    private BigDecimal principal;
    private Integer loanTermInMonths;
    private BigDecimal nominalAnnualInterestRate;
    private LocalDate disbursementDate;
    private LocalDate submittedOnDate;
    private String externalId;
    private List<ChargePayload> charges;
    private CollateralPayload collateral;
    private List<GuarantorPayload> guarantors;
    private DisbursementPayload disbursementDetail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargePayload {
        private Long chargeId;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralPayload {
        private Long collateralTypeId;
        private String collateralValue;
        private String collateralQuality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuarantorPayload {
        private Long clientId;
        private BigDecimal guarantorAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisbursementPayload {
        private String date;
        private BigDecimal transactionAmount;
        private Long paymentTypeId;
        private String accountNumber;
        private String externalRef;
    }
}