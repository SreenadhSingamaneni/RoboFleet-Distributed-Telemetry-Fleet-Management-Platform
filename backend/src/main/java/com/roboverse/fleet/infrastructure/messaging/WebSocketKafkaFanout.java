package com.roboverse.fleet.infrastructure.messaging;

import jakarta.validation.Validator;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketKafkaFanout {
    private final SimpMessagingTemplate messagingTemplate;
    private final Validator validator;

    public WebSocketKafkaFanout(SimpMessagingTemplate messagingTemplate, Validator validator) {
        this.messagingTemplate = messagingTemplate;
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${fleet.topics.telemetry}",
            groupId = "${fleet.websocket.consumer-group}",
            containerFactory = "telemetryKafkaListenerContainerFactory")
    public void telemetry(
            List<ConsumerRecord<String, TelemetryEvent>> records,
            Acknowledgment acknowledgment) {
        var valid = records.stream()
                .map(ConsumerRecord::value)
                .filter(event -> event != null && event.schemaVersion() == 1)
                .filter(event -> validator.validate(event).isEmpty())
                .map(TelemetryEvent::toDomain)
                .toList();
        if (!valid.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/telemetry", valid);
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "${fleet.topics.alerts}",
            groupId = "${fleet.websocket.consumer-group}-alerts",
            containerFactory = "alertFanoutKafkaListenerContainerFactory")
    public void alerts(AlertEvent event) {
        messagingTemplate.convertAndSend("/topic/alerts", List.of(event));
    }
}

