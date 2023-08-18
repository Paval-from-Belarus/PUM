package org.petos.pum.server.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Paval Shlyk
 * @since 18/08/2023
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ManagerRepository implements StorageDao {
public ManagerRepository(EntityManager manager) {
	this.manager = manager;
}
@PersistenceContext
private final EntityManager manager;
}
