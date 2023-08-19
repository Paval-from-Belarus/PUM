package org.petos.pum.server.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author Paval Shlyk
 * @since 18/08/2023
 */
@Repository
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Transactional
public class StorageDaoImpl implements StorageDao<Integer, PackageAlias> {

public StorageDaoImpl(EntityManager manager) {
	this.manager = manager;
	doSomething();
}
public void doSomething() {
      var transaction = manager.getTransaction();
      transaction.begin();
      transaction.commit();
}
@PersistenceContext
private final EntityManager manager;
}
