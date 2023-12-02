package org.petos.pum.publications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@SpringBootApplication
@EnableWebFlux
public class PublicationApplication {
public static void main(String[] args) {
      SpringApplication.run(PublicationApplication.class, args);
}
}