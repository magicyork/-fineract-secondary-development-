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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationEvent {

    private String eventId;
    private String eventType;
    private String sourceSystem;
    private String targetSystem;
    private String correlationId;
    private LocalDateTime timestamp;
    private String payload;
    private String status;
    private Integer retryCount;

    public static IntegrationEvent loanCreated(String correlationId, Long loanId, String coreApplicationId) {
        return IntegrationEvent.builder()
                .eventId("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .eventType("LOAN_CREATED")
                .sourceSystem("FINERACT")
                .targetSystem("CORE_BANKING")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .payload("{\"loanId\":" + loanId + ",\"coreApplicationId\":\"" + coreApplicationId + "\"}")
                .status("NEW")
                .retryCount(0)
                .build();
    }

    public static IntegrationEvent loanDisbursed(String correlationId, Long loanId, BigDecimal amount) {
        return IntegrationEvent.builder()
                .eventId("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .eventType("LOAN_DISBURSED")
                .sourceSystem("FINERACT")
                .targetSystem("CORE_BANKING")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .payload("{\"loanId\":" + loanId + ",\"disbursedAmount\":" + amount + "}")
                .status("NEW")
                .retryCount(0)
                .build();
    }

    public static IntegrationEvent repaymentReceived(String correlationId, Long loanId, BigDecimal amount) {
        return IntegrationEvent.builder()
                .eventId("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .eventType("REPAYMENT_RECEIVED")
                .sourceSystem("FINERACT")
                .targetSystem("CORE_BANKING")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .payload("{\"loanId\":" + loanId + ",\"repaymentAmount\":" + amount + "}")
                .status("NEW")
                .retryCount(0)
                .build();
    }

    public static IntegrationEvent clientKycUpdated(String correlationId, Long clientId) {
        return IntegrationEvent.builder()
                .eventId("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .eventType("CLIENT_KYC_UPDATED")
                .sourceSystem("CORE_BANKING")
                .targetSystem("FINERACT")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .payload("{\"clientId\":" + clientId + "}")
                .status("NEW")
                .retryCount(0)
                .build();
    }
}