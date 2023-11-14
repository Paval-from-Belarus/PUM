package org.petos.pum.http;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 02/11/2023
 */
@Configuration
public class KafkaConsumerConfig {
@Value("${spring.kafka.bootstrap-servers}")
private String kafkaServer;
@Value("${spring.kafka.consumer.group-id}")
private String kafkaGroupId;
@Value("${kafka.schema}")
private String schemaUrl;

//@Bean
//public KafkaListenerContainerFactory<?> headerInfoFactory(ConsumerFactory<String, PackageInfo> factory) {
////    public KafkaListenerContainerFactory<?> headInfoFactory() {
//      ConcurrentKafkaListenerContainerFactory<String, PackageInfo> containerFactory =
//	  new ConcurrentKafkaListenerContainerFactory<>();
//      Map<String, Object> properties = factory.getConfigurationProperties();
//      System.out.println(properties);
//      containerFactory.setConsumerFactory(factory);
//      return containerFactory;
//}
//
//@Bean
//public ConsumerFactory<String, PackageInfo> consumerFactory() {
//      return new DefaultKafkaConsumerFactory<>(consumerConfigs());
//}

public Map<String, Object> consumerConfigs() {
      Map<String, Object> props = new HashMap<>();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer");
      props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
      props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
      props.put(KafkaProtobufDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
      return props;
}
}
