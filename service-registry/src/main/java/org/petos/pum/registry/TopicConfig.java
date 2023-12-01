package org.petos.pum.registry;

import org.apache.kafka.clients.admin.NewTopic;
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
public NewTopic packageRequestsTopic() {
      return TopicBuilder
		 .name("package-requests")
		 .build();
}

@Bean
public NewTopic packageInfoTopic() {
      return TopicBuilder
		 .name("package-info")
		 .build();
}

@Bean
public NewTopic publisherRequestsTopic() {
      return TopicBuilder
		 .name("publisher-requests")
		 .build();
}

@Bean
public NewTopic publisherInfoTopic() {
      return TopicBuilder
		 .name("publisher-info")
		 .build();
}
}
