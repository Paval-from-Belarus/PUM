package org.petos.pum.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @author Paval Shlyk
 * @since 13/11/2023
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {
public static void main(String[] args) {
      SpringApplication.run(ServiceRegistryApplication.class, args);
}

}