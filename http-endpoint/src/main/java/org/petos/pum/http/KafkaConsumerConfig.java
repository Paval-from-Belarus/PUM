package org.petos.pum.http;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.petos.pum.networks.dto.packages.HeaderInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 02/11/2023
 */
@Configuration
public class KafkaConsumerConfig {
@Value("${kafka.server}")
private String kafkaServer;
@Value("${kafka.group.id}")
private String kafkaGroupId;

@Bean
public KafkaListenerContainerFactory<?> headerInfoFactory() {
      ConcurrentKafkaListenerContainerFactory<Long, HeaderInfo> factory =
	  new ConcurrentKafkaListenerContainerFactory<>();
//      factory.setConsumerFactory(co);
//      ConcurrentKafkaListenerContainerFactory<Long, HeaderRequest>
      return null;
}

@Bean
public ConsumerFactory<Long, HeaderInfo> consumerFactory() {
      return new DefaultKafkaConsumerFactory<>(consumerConfigs());
}

@Bean
public Map<String, Object> consumerConfigs() {
      Map<String, Object> props = new HashMap<>();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
      props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
      props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
      return props;
}
}
