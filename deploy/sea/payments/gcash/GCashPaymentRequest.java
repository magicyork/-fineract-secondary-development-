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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GCash Payment Request Data Object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GCashPaymentRequest {

    /**
     * Unique order ID (merchant reference).
     * Format: ORD{yyyyMMddHHmmss}{random}
     */
    private String orderId;

    /**
     * Payment amount in PHP.
     */
    private BigDecimal amount;

    /**
     * Payment type: APP_PAY, QR_PAY, WEB_PAY, TRANSFER
     */
    private String paymentType;

    /**
     * Payment description (shown to user).
     */
    private String description;

    /**
     * Buyer/GCash user ID (for APP_PAY).
     */
    private String buyerId;

    /**
     * Receiver phone number (for TRANSFER).
     */
    private String receiverPhone;

    /**
     * Receiver name (for TRANSFER).
     */
    private String receiverName;

    /**
     * Bank code for bank transfer (for TRANSFER).
     * Reference: Philippine Bank Codes (BSP).
     */
    private String bankCode;

    /**
     * Account name for bank transfer.
     */
    private String accountName;

    /**
     * Callback URL for payment result notification.
     */
    private String callbackUrl;

    /**
     * Redirect URL after payment completion (for WEB_PAY).
     */
    private String redirectUrl;

    /**
     * Extended parameters for future use.
     */
    private Map<String, Object> extParams;
}
