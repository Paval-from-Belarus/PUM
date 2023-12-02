package org.petos.pum.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class RepositoryApplication {
@Bean(name = "fileTaskExecutor")
public Executor fileTaskExecutor() {
      return Executors.newFixedThreadPool(10);
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
//@Bean
//public ProducerFactory<String, PackageInfo> requestProducerFactory() {
//      return new DefaultKafkaProducerFactory<>(producerConfig());
//}

public static void main(String[] args) {
      SpringApplication.run(RepositoryApplication.class, args);
}
}