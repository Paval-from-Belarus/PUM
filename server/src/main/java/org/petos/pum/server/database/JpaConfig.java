package org.petos.pum.server.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * @author Paval Shlyk
 * @since 19/08/2023
 */
@Configuration
@EntityScan
@EnableJpaRepositories
@EnableTransactionManagement
public class JpaConfig {
public JpaConfig(DataSource dataSource) {
      this.dataSource = dataSource;
}

@Bean
public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
      JpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(this.dataSource);
      factory.setPackagesToScan();
      //factory.setPackagesToScan is redundant because @EntityScan is enabled
      factory.setJpaVendorAdapter(adapter);
      factory.setJpaProperties(jpaProperties());
      return factory;
}

private Properties jpaProperties() {
      Map<String, String> vendorProperties = Map.of(
	  "hibernate.c3po.min_size", "5",
	  "hibernate.c3po.max_size", "30",
	  "hibernate.c3po.timeout", "1500",
	  "hbm2dll.auto", "update",
	  "show_sql", "true",
	  "dialect", "org.hibernate.dialect.HSQLDialect"
      );
      var properties = new Properties();
      properties.putAll(vendorProperties);
      return properties;
}

private final DataSource dataSource;

}
