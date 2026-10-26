package com.synthetic.readiness.stabilisation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
class StabilisationEventConfiguration {
    @Bean
    NewTopics phaseTwoTopics() {
        return new NewTopics(
            TopicBuilder.name("payment-status-events").partitions(1).replicas(1).build(),
            TopicBuilder.name("stabilisation-exceptions").partitions(1).replicas(1).build());
    }
}
