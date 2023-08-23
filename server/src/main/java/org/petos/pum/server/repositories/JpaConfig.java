package org.petos.pum.server.repositories;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
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
@Autowired
public JpaConfig(DataSource dataSource) {
      this.dataSource = dataSource;
}

@Bean
public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
      JpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(this.dataSource);
      //shouldn't pass any package name because @EntityScan is marked
      //in future should consider to use PersistenceManagedTypes instead
      factory.setPackagesToScan("org.petos.pum.server.repositories");
      factory.setJpaVendorAdapter(adapter);
      factory.setJpaProperties(jpaProperties());
      return factory;
}

//@Bean
//public PlatformTransactionManager
//transactionManager(EntityManagerFactory emf) {
//      JpaTransactionManager transactionManager = new JpaTransactionManager();
//      transactionManager.setEntityManagerFactory(emf);
//      return transactionManager;
//}
//@Bean
//public PersistenceExceptionTranslationPostProcessor
//exceptionTranslation(){
//      return new PersistenceExceptionTranslationPostProcessor();
//}
//consider in future to implements own PlatformTransactionManager and own PersistenceExceptionBeanPostProcessor
//now -> Spring boot automatically create such instances
private Properties jpaProperties() {
      Map<String, String> vendorProperties = Map.of(
	  "hibernate.c3po.min_size", "5",
	  "hibernate.c3po.max_size", "30",
	  "hibernate.c3po.timeout", "1500",
	  "hibernate.dialect", "org.hibernate.dialect.HSQLDialect"
      );
      var properties = new Properties();
      properties.putAll(vendorProperties);
      return properties;
}

private final DataSource dataSource;
}
