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
public class CoreLoanApplicationDTO {

    private String applicationId;
    private String customerId;
    private String productCode;
    private BigDecimal loanAmount;
    private LocalDate applicationDate;
    private LocalDate expectedDisbursementDate;
    private String repaymentMethod;
    private Integer repaymentPeriod;
    private String collateralType;
    private BigDecimal collateralValue;
    private String guarantorId;
    private String purpose;
    private String branchCode;
    private String officerCode;
    private Boolean approved;
    private String approvalStatus;
    private String rejectionReason;
    private List<RepaymentScheduleItem> schedule;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepaymentScheduleItem {
        private Integer installmentNo;
        private LocalDate dueDate;
        private BigDecimal principalAmount;
        private BigDecimal interestAmount;
        private BigDecimal totalAmount;
    }
}