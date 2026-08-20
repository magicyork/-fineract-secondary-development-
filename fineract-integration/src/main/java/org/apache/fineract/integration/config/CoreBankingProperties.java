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
package org.apache.fineract.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fineract.integration.core")
public class CoreBankingProperties {

    private boolean enabled = false;
    private String baseUrl = "https://core-bank.example.com";
    private String apiPathPrefix = "/api/v1";
    private String tenantId = "default";
    private String clientId = "fineract-credit";
    private String clientSecret;
    private String tokenUrl = "/oauth/token";
    private long connectTimeoutMs = 5000;
    private long readTimeoutMs = 30000;
    private long writeTimeoutMs = 30000;

    private DataSyncConfig dataSync = new DataSyncConfig();
    private EventConfig event = new EventConfig();

    @Data
    public static class DataSyncConfig {
        private boolean clientSyncEnabled = true;
        private boolean accountSyncEnabled = true;
        private boolean productSyncEnabled = true;
        private String clientSyncCron = "0 0/5 * * * ?";
        private String accountSyncCron = "0 0/10 * * * ?";
    }

    @Data
    public static class EventConfig {
        private boolean kafkaEnabled = true;
        private String bootstrapServers = "localhost:9092";
        private String coreEventTopic = "core-banking-events";
        private String fineractEventTopic = "fineract-credit-events";
        private String consumerGroup = "fineract-integration";
    }
}