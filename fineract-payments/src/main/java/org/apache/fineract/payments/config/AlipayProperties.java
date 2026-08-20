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

@Data
@ConfigurationProperties(prefix = "fineract.payments.alipay")
public class AlipayProperties {

    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    private String appId;

    private String privateKey;

    private String alipayPublicKey;

    private String signType = "RSA2";

    private String charset = "UTF-8";

    private String notifyUrl = "https://your-domain.com/api/v1/payments/alipay/callback";

    private int timeout = 30;

    private boolean enabled = true;
}
