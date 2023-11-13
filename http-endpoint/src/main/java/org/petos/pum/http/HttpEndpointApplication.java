package org.petos.pum.http;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@SpringBootApplication
public class HttpEndpointApplication {
@KafkaListener(
    topics = "package-info",
    groupId = "first"
)
public void listener(PackageInfo info) {
      System.out.println(info);
}
//@Bean
//public Map<String, Object> producerConfigs() {
//      Map<String, Object> props = new HashMap<>();
//      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
////      props.put(ProducerConfig.)
////      props.put(ProducerConfig.)
//      return props;
//}
//@Bean
//public RouteLocator myRoutes(RouteLocatorBuilder builder) {
//      return builder.routes().build();
////      return builder.routes()
////		 .route(p -> p
////				 .path("/get")
////				 .filters(f -> f.circuitBreaker(config -> config.setName("mycmd")))
////				 .uri("http://httpbin.org:80"))
////		 .build();
//}

//@Bean
//public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//      return builder.routes()
//                 .route("r1", r -> r.host("**.baeldung.com")
//                                       .and()
//                                       .path("/baeldung")
//                                       .uri("http://baeldung.com"))
//                 .route(r -> r.host("**.baeldung.com")
//                                 .and()
//                                 .path("/myOtherRouting")
//                                 .filters(f -> f.prefixPath("/myPrefix"))
//                                 .uri("http://othersite.com")
//                                 .id("myOtherID"))
//                 .build();
//}
public static void main(String[] args) {
      SpringApplication.run(HttpEndpointApplication.class, args);
}
}