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

import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.config.CoreBankingProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenManager {

    private final CoreBankingProperties properties;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        refreshToken();
        return cachedToken;
    }

    private void refreshToken() {
        log.debug("Refreshing OAuth2 token for core banking integration");
        try {
            String credentials = properties.getClientId() + ":" + properties.getClientSecret();
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

            cachedToken = basicAuth;
            tokenExpiry = Instant.now().plusSeconds(3600);
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            cachedToken = null;
            tokenExpiry = Instant.now();
        }
    }
}