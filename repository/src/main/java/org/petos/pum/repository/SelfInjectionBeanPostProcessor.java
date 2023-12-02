package org.petos.pum.repository;

import org.petos.pum.repository.service.impl.RepositoryServiceImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author Paval Shlyk
 * @since 02/12/2023
 */
@Component
public class SelfInjectionBeanPostProcessor implements BeanPostProcessor {
@Override
public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof RepositoryServiceImpl service) {
	    service.setSelfInjection(service);
      }
      return bean;
}
}
