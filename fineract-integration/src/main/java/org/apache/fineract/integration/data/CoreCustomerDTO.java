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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreCustomerDTO {

    private String customerId;
    private String customerName;
    private String customerType;
    private String idType;
    private String idNumber;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String address;
    private String riskLevel;
    private String creditRating;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private String officeId;
    private String status;
    private String extClientId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoreAccountDTO {
        private String accountNo;
        private String accountType;
        private String accountStatus;
        private BigDecimal balance;
        private BigDecimal availableBalance;
        private String currency;
        private LocalDate openDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoreProductDTO {
        private String productCode;
        private String productName;
        private String productType;
        private BigDecimal interestRate;
        private String interestMethod;
        private Integer termMonths;
        private BigDecimal maxAmount;
        private BigDecimal minAmount;
        private String status;
    }
}