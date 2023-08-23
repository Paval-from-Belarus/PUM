package org.petos.pum.server.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateError;
import org.petos.pum.server.repositories.entities.PackageAlias;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.data.repository.CrudRepository;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 22/08/2023
 */
//@Repository("managedPackageHatDao")
//@Scope(BeanDefinition.SCOPE_PROTOTYPE)

public class PackageHatDaoImpl implements PackageHatDao {
@Autowired
public PackageHatDaoImpl(EntityManager manager) {
      this.manager = manager;
}

@PersistenceContext
private final EntityManager manager;

@Override
public Iterable<PackageHat> findAll() {
      return null;
}

@Override
public Iterable<PackageHat> findByPublisherId(Integer authorId) {
      return null;
}

@Override
public Optional<PackageHat> findByIdAndValid(Integer id) {
      return Optional.empty();
}

@Override
public Optional<PackageHat> findById(Integer id) {
      return Optional.empty();
}

@Override
public long count() {
      throw new HibernateError("hello");
}

@Override
public PackageHat save(PackageHat entity) {
      return null;
}

@Override
public void delete(PackageHat entity) {

}

@Override
public void deleteById(Integer id) {

}

@Override
public void deleteByPublisher(Integer authorId) {

}

@Override
public void deleteAll() {

}

@Override
public boolean existsById(Integer id) {
      return false;
}

@Override
public boolean existsByPublisher(Integer authorId) {
      return false;
}
}
