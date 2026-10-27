package com.ceo.trading_platform_backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

class KafkaConfigTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
            .withPropertyValues("KAFKA_BOOTSTRAP_SERVERS=broker.example:9092");

    @Test
    void buildsProducerAndConsumerFromApplicationProperties() {
        contextRunner.run(context -> {
            assertThat(context.getBean(ProducerFactory.class).getConfigurationProperties())
                    .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("broker.example:9092"))
                    .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
            assertThat(context.getBean(ConsumerFactory.class).getConfigurationProperties())
                    .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("broker.example:9092"))
                    .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class)
                    .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            assertThat(context.getBean(ConcurrentKafkaListenerContainerFactory.class)
                    .getContainerProperties().getAckMode()).isEqualTo(AckMode.BATCH);
        });
    }

    @Test
    void injectsTypedKafkaTemplateIntoProducers() {
        contextRunner.withBean(TemplateConsumer.class)
                .run(context -> assertThat(context.getBean(TemplateConsumer.class).kafka()).isNotNull());
    }

    record TemplateConsumer(KafkaTemplate<String, UUID> kafka) {}
}
