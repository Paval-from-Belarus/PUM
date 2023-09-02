package org.petos.pum.server.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Builder;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public class StorageDaoImpl implements CrudRepository<PackageHat, Long> {
@Autowired
public StorageDaoImpl(EntityManager manager) {
      this.manager = manager;
//	doSomething();
}

//@Query
//public void doSomething() {
/* no-op */
//}
@PersistenceContext
private final EntityManager manager;

@Override
public <S extends PackageHat> S save(S entity) {
      return null;
}

@Override
public <S extends PackageHat> Iterable<S> saveAll(Iterable<S> entities) {
      return null;
}

@Builder
@Override
public Optional<PackageHat> findById(Long aLong) {
      return Optional.empty();
}

@Override
public boolean existsById(Long aLong) {
      return false;
}

@Override
public Iterable<PackageHat> findAll() {
      return null;
}

@Override
public Iterable<PackageHat> findAllById(Iterable<Long> longs) {
      return null;
}

@Override
public long count() {
      return 0;
}

@Override
public void deleteById(Long aLong) {

}

@Override
public void delete(PackageHat entity) {

}

@Override
public void deleteAllById(Iterable<? extends Long> longs) {

}

@Override
public void deleteAll(Iterable<? extends PackageHat> entities) {

}

@Override
public void deleteAll() {

}
}
