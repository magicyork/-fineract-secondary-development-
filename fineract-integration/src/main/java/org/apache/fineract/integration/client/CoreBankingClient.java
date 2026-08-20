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
package org.apache.fineract.integration.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.config.CoreBankingProperties;
import org.apache.fineract.integration.data.CoreCustomerDTO;
import org.apache.fineract.integration.infrastructure.IntegrationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreBankingClient {

    private final CoreBankingProperties properties;
    private final RestTemplate restTemplate;
    private final TokenManager tokenManager;

    public CoreCustomerDTO getCustomer(String customerId) {
        log.debug("Fetching customer from core banking: customerId={}", customerId);
        String url = buildUrl("/customers/" + customerId);
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CoreCustomerDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CoreCustomerDTO.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch customer from core: {}", e.getMessage());
            throw new IntegrationException("core.customer.fetch.failed", "Failed to fetch customer: " + customerId, e);
        }
    }

    public List<CoreCustomerDTO> getCustomersByDate(LocalDate lastModifiedDate) {
        log.debug("Fetching customers modified after: {}", lastModifiedDate);
        String url = buildUrl("/customers") + "?lastModifiedDate=" + lastModifiedDate;
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CoreCustomerDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CoreCustomerDTO[].class);
            return List.of(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch customers: {}", e.getMessage());
            throw new IntegrationException("core.customer.list.failed", "Failed to fetch customers", e);
        }
    }

    public List<CoreCustomerDTO.CoreAccountDTO> getCustomerAccounts(String customerId) {
        log.debug("Fetching accounts for customer: {}", customerId);
        String url = buildUrl("/customers/" + customerId + "/accounts");
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CoreCustomerDTO.CoreAccountDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CoreCustomerDTO.CoreAccountDTO[].class);
            return List.of(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch accounts: {}", e.getMessage());
            throw new IntegrationException("core.account.fetch.failed", "Failed to fetch accounts for: " + customerId, e);
        }
    }

    public String notifyLoanDisbursement(String coreApplicationId, String fineractLoanId, BigDecimal amount) {
        log.info("Notifying core banking of loan disbursement: appId={}, loanId={}, amount={}",
                coreApplicationId, fineractLoanId, amount);
        String url = buildUrl("/loans/disbursements/notify");
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String payload = String.format(
                "{\"coreApplicationId\":\"%s\",\"fineractLoanId\":\"%s\",\"amount\":%s,\"status\":\"DISBURSED\"}",
                coreApplicationId, fineractLoanId, amount);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to notify core banking: {}", e.getMessage());
            throw new IntegrationException("core.loan.notify.failed",
                    "Failed to notify core banking of disbursement", e);
        }
    }

    public String notifyRepayment(String coreApplicationId, String fineractLoanId, BigDecimal amount, String type) {
        log.info("Notifying core banking of repayment: appId={}, loanId={}, amount={}, type={}",
                coreApplicationId, fineractLoanId, amount, type);
        String url = buildUrl("/loans/repayments/notify");
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String payload = String.format(
                "{\"coreApplicationId\":\"%s\",\"fineractLoanId\":\"%s\",\"amount\":%s,\"repaymentType\":\"%s\"}",
                coreApplicationId, fineractLoanId, amount, type);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to notify core banking: {}", e.getMessage());
            throw new IntegrationException("core.repayment.notify.failed",
                    "Failed to notify core banking of repayment", e);
        }
    }

    private String buildUrl(String path) {
        return properties.getBaseUrl() + properties.getApiPathPrefix() + path;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", properties.getTenantId());
        headers.set("X-Client-ID", properties.getClientId());
        String token = tokenManager.getAccessToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}