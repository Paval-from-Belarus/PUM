package org.petos.pum.http;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@SpringBootApplication
public class HttpEndpointApplication {
@Value("${kafka.server}")
private String kafkaServer;
@Value("${kafka.group.id}")
private String kafkaGroupId;

@Bean
public Map<String, Object> producerConfigs() {
      Map<String, Object> props = new HashMap<>();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
//      props.put(ProducerConfig.)
//      props.put(ProducerConfig.)
      return props;

}

public static void main(String[] args) {
      SpringApplication.run(HttpEndpointApplication.class, args);
}
}