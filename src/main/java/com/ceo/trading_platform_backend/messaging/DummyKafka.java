package com.ceo.trading_platform_backend.messaging;

import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

public class DummyKafka {
    
    public static EmbeddedKafkaKraftBroker broker;

    public static void start() {
        broker = new EmbeddedKafkaKraftBroker(1, 3, "orders.unvalidated", "orders.unexecuted");
        broker.brokerListProperty("spring.kafka.bootstrap-servers");
        broker.afterPropertiesSet();
        Runtime.getRuntime().addShutdownHook(new Thread(new StopKafka()));
    }

    private static class StopKafka implements Runnable {
        @Override public void run() {
            broker.destroy();
        }
    }

}
