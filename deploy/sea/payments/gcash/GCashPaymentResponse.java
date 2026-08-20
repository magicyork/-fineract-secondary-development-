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
package org.apache.fineract.payments.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GCash Payment Response Data Object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GCashPaymentResponse {

    /**
     * Result code: "SUCCESS" or error code.
     * Success codes: SUCCESS, ACCEPTED
     * Error codes: INVALID_PARAM, EXPIRED, FAILED, CANCELLED, etc.
     */
    @JsonProperty("resultCode")
    private String resultCode;

    /**
     * Result message / description.
     */
    @JsonProperty("resultMessage")
    private String resultMessage;

    /**
     * Reference ID from GCash (for successful payments).
     */
    @JsonProperty("referenceId")
    private String referenceId;

    /**
     * Merchant order ID (echoed back).
     */
    @JsonProperty("orderId")
    private String orderId;

    /**
     * Payment status: SUCCESS, PENDING, FAILED, CANCELLED
     */
    @JsonProperty("status")
    private String status;

    /**
     * Payment amount confirmed.
     */
    @JsonProperty("amount")
    private Double amount;

    /**
     * Currency code.
     */
    @JsonProperty("currency")
    private String currency;

    /**
     * Authorized code (for card payments).
     */
    @JsonProperty("authCode")
    private String authCode;

    /**
     * QR code URL (for QR_PAY type).
     */
    @JsonProperty("qrCodeUrl")
    private String qrCodeUrl;

    /**
     * Checkout URL (for WEB_PAY type).
     */
    @JsonProperty("checkoutUrl")
    private String checkoutUrl;

    /**
     * Timestamp of the response.
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Whether the operation was successful.
     */
    private boolean success;
}
