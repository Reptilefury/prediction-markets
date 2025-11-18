package com.oregonMarkets.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishEvent(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published event to topic {}: {}", topic, key);
                    } else {
                        log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing event to topic {}: {}", topic, e.getMessage());
        }
    }
}