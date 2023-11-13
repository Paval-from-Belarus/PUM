package org.petos.pum.registry;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.internals.Topic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * The configuration file for the whole application
 *
 * @author Paval Shlyk
 * @since 13/11/2023
 */
@Configuration
public class TopicConfig {
@Bean
public NewTopic topic() {
      return TopicBuilder.name("client").build();
}
}
