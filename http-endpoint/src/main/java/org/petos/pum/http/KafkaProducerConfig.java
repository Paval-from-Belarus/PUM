package org.petos.pum.http;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 02/11/2023
 */
@Configuration
public class KafkaProducerConfig {

//@Value("${spring.kafka.bootstrap-servers}")
//private String bootstrapServers;
//@Value("${kafka.schema}")
//private String schemaUrl;
//@Value("${spring.kafka.consumer.group-id}")
//private String kafkaConsumerId;
//
//@Bean
//public Map<String, Object> producerConfig() {
//      Map<String, Object> props = new HashMap<>();
//      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
//      props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaConsumerId);
//      props.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
//      props.put(KafkaProtobufSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicNameStrategy.class);
//      return props;
//}
//
////@Autowired
////ProducerFactory<?, ?> defaultFactory;
////
////@Bean
////public ProducerFactory<String, PackageRequest> requestProducerFactory() {
////      Map<String, Object> properties = defaultFactory.getConfigurationProperties();
////      return new DefaultKafkaProducerFactory<>(producerConfig());
////}


@Bean
public KafkaTemplate<String, PackageRequest> requestKafkaTemplate(ProducerFactory<?, ?> defaultFactory) {
      Map<String, Object> properties = defaultFactory.getConfigurationProperties();
      return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties));
}

}