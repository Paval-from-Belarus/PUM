package org.petos.pum.repository;

import com.google.protobuf.DynamicMessage;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.petos.pum.networks.dto.packages.HeaderInfo;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@SpringBootApplication
public class RepositoryApplication {
//@Bean
//public KafkaListenerContainerFactory<?> batchFactory() {
//      ConcurrentKafkaListenerContainerFactory<String, PackageRequest> factory =
//	  new ConcurrentKafkaListenerContainerFactory<>();
//      factory.setConsumerFactory(consumerFactory());
//      return factory;
//}
//
//@Bean
//public ConsumerFactory<String, PackageRequest> consumerFactory() {
//      return new DefaultKafkaConsumerFactory<>(consumerConfig());
//}
//
//
//@Bean
//public Map<String, Object> consumerConfig() {
//      Map<String, Object> props = new HashMap<>();
//      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
//      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
//      props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
//      props.put(KafkaProtobufDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
//      props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//      return props;
//}

@Value("${spring.kafka.bootstrap-servers}")
private String bootstrapServers;
@Value("${kafka.schema}")
private String schemaUrl;
@Value("${spring.kafka.consumer.group-id}")
private String kafkaConsumerId;

@Bean
public Map<String, Object> producerConfig() {
      Map<String, Object> props = new HashMap<>();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
      props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaConsumerId);
      props.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
      props.put(KafkaProtobufSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicNameStrategy.class);
      return props;
}

@Bean
public ProducerFactory<String, PackageInfo> requestProducerFactory() {
      return new DefaultKafkaProducerFactory<>(producerConfig());
}


@Bean
public KafkaTemplate<String, PackageInfo> requestKafkaTemplate() {
      return new KafkaTemplate<>(requestProducerFactory());
}

public static void main(String[] args) {
      SpringApplication.run(RepositoryApplication.class, args);
}
}