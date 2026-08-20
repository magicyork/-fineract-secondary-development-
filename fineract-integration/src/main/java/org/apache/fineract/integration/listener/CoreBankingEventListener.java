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
package org.apache.fineract.integration.listener;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.data.IntegrationEvent;
import org.apache.fineract.integration.publisher.IntegrationEventPublisher;
import org.apache.fineract.infrastructure.event.business.BusinessEventListener;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanApprovedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanCreatedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanDisbursedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.repayment.LoanRepaymentBusinessEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreBankingEventListener {

    private final IntegrationEventPublisher eventPublisher;

    public void onLoanCreated(LoanCreatedBusinessEvent event) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Loan created event received: loanId={}, correlationId={}", event.getLoanId(), correlationId);

        IntegrationEvent integrationEvent = IntegrationEvent.loanCreated(
                correlationId, event.getLoanId(), event.getExternalId());
        eventPublisher.publishEvent(integrationEvent);
    }

    public void onLoanApproved(LoanApprovedBusinessEvent event) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Loan approved event received: loanId={}", event.getLoanId());

        IntegrationEvent eventOut = IntegrationEvent.builder()
                .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .eventType("LOAN_APPROVED")
                .sourceSystem("FINERACT")
                .targetSystem("CORE_BANKING")
                .correlationId(correlationId)
                .timestamp(java.time.LocalDateTime.now())
                .payload("{\"loanId\":" + event.getLoanId() + "}")
                .status("NEW")
                .retryCount(0)
                .build();
        eventPublisher.publishEvent(eventOut);
    }

    public void onLoanDisbursed(LoanDisbursedBusinessEvent event) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Loan disbursed event received: loanId={}", event.getLoanId());

        IntegrationEvent disbursedEvent = IntegrationEvent.loanDisbursed(
                correlationId, event.getLoanId(), BigDecimal.ZERO);
        eventPublisher.publishEvent(disbursedEvent);
    }

    public void onRepaymentReceived(LoanRepaymentBusinessEvent event) {
        String correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Repayment received event: loanId={}", event.getLoanId());

        IntegrationEvent repaymentEvent = IntegrationEvent.repaymentReceived(
                correlationId, event.getLoanId(), event.getAmount());
        eventPublisher.publishEvent(repaymentEvent);
    }
}