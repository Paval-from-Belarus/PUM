package org.petos.pum.repository;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
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
@Value("${spring.kafka.bootstrap-servers}")
private String kafkaServer;
@Value("${spring.kafka.consumer.group-id}")
private String kafkaGroupId;
@Value("${kafka.schema}")
private String schemaUrl;

@KafkaListener(
    topics = "package-requests",
    groupId = "first"
)
public void listen(GenericMessage message) {
      System.out.println(message.getPayload().getClass());

}

public void listener() {
//      final Consumer<String, Message> consumer = new KafkaConsumer<>(consumerConfig());
//      consumer.subscribe(List.of("package-requests"));
//      try {
//	    ConsumerRecords<String, Message> messages = consumer.poll(1000);
//	    for (ConsumerRecord<String, Message> message : messages) {
//		  System.out.println(message.value());
//	    }
//      } catch (Exception e) {
//	    consumer.close();
//      }

}

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

public static void main(String[] args) {
      SpringApplication.run(RepositoryApplication.class, args);
}
}