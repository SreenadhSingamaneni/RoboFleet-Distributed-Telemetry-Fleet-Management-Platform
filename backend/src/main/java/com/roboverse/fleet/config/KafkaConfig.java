package com.roboverse.fleet.config;

import com.roboverse.fleet.infrastructure.messaging.AlertEvent;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {
    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> telemetryKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            KafkaTemplate<Object, Object> kafkaTemplate) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".dlt", record.partition()));
        var backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(500);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(4_000);
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, backoff));
        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, AlertEvent> alertFanoutKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties(null);
        var deserializer = new JsonDeserializer<>(AlertEvent.class, false);
        deserializer.addTrustedPackages("com.roboverse.fleet.infrastructure.messaging");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), deserializer);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, AlertEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    NewTopic telemetryTopic(FleetProperties properties) {
        return TopicBuilder.name(properties.topics().telemetry()).partitions(12).build();
    }

    @Bean
    NewTopic telemetryInvalidTopic(FleetProperties properties) {
        return TopicBuilder.name(properties.topics().telemetryInvalid()).partitions(3).build();
    }

    @Bean
    NewTopic telemetryDeadLetterTopic(FleetProperties properties) {
        return TopicBuilder.name(properties.topics().telemetry() + ".dlt").partitions(12).build();
    }

    @Bean
    NewTopic alertsTopic(FleetProperties properties) {
        return TopicBuilder.name(properties.topics().alerts()).partitions(6).build();
    }
}
