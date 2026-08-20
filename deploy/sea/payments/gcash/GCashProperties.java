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
package org.apache.fineract.payments.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GCash Payment Gateway Configuration Properties.
 * Configured via environment variables:
 *   GCASH_ENABLED, GCASH_GATEWAY_URL, GCASH_PARTNER_ID, GCASH_SSO_KEY, etc.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fineract.payments.gcash")
public class GCashProperties {

    private boolean enabled = false;

    /**
     * GCash API base URL.
     * Production: https://api.gcash.com/merchant/v1
     * Sandbox: https://sandbox-api.gcash.com/merchant/v1
     */
    private String gatewayUrl = "https://api.gcash.com/merchant/v1";

    /**
     * Merchant/Partner ID assigned by GCash.
     */
    private String partnerId;

    /**
     * SSO Key used for HMAC-SHA256 signature generation.
     * Store this securely (use Kubernetes Secrets or AWS Secrets Manager).
     */
    private String ssoKey;

    /**
     * Callback URL for payment notifications.
     */
    private String notifyUrl;

    /**
     * Timeout in seconds for API calls.
     */
    private int timeout = 30;

    /**
     * Payment types supported:
     *  - APP_PAY: In-app payment via GCash App
     *  - QR_PAY: QR code payment
     *  - WEB_PAY: Web-based payment via GCash
     *  - TRANSFER: Fund transfer to bank/e-wallet
     */
    private String supportedPaymentTypes = "APP_PAY,QR_PAY,WEB_PAY,TRANSFER";
}
