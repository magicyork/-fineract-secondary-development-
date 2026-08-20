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
package org.apache.fineract.integration.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integration.config.CoreBankingProperties;
import org.apache.fineract.integration.data.IntegrationEvent;
import org.apache.fineract.integration.infrastructure.IntegrationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventPublisher {

    private final CoreBankingProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Async
    public void publishEvent(IntegrationEvent event) {
        if (!properties.getEvent().isKafkaEnabled()) {
            log.debug("Kafka integration disabled, skipping event publish: {}", event.getEventType());
            return;
        }

        try {
            String topic = properties.getEvent().getFineractEventTopic();
            String key = event.getCorrelationId();
            String value = serializeEvent(event);

            log.info("Publishing integration event: type={}, topic={}, correlationId={}",
                    event.getEventType(), topic, key);

            kafkaTemplate.send(topic, key, value)
                    .addCallback(
                            result -> log.debug("Event published successfully: eventId={}", event.getEventId()),
                            ex -> log.error("Failed to publish event: eventId={}, error={}",
                                    event.getEventId(), ex.getMessage())
                    );
        } catch (Exception e) {
            log.error("Failed to publish integration event: {}", e.getMessage());
            throw new IntegrationException("event.publish.failed",
                    "Failed to publish event: " + event.getEventType(), e);
        }
    }

    private String serializeEvent(IntegrationEvent event) {
        return String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"%s\",\"source\":\"%s\",\"target\":\"%s\",\"correlationId\":\"%s\",\"timestamp\":\"%s\",\"payload\":%s,\"status\":\"%s\"}",
                event.getEventId(),
                event.getEventType(),
                event.getSourceSystem(),
                event.getTargetSystem(),
                event.getCorrelationId(),
                event.getTimestamp(),
                event.getPayload(),
                event.getStatus()
        );
    }
}