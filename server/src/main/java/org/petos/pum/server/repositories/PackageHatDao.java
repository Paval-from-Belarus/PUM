package org.petos.pum.server.repositories;


import org.petos.pum.server.repositories.entities.PackageHat;
import org.petos.pum.server.repositories.entities.PublisherInfo;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;

import java.util.Optional;

/**
 * This dao interact with PackageHat entity
 * @author Paval Shlyk
 * @since 19/08/2023
 */
public interface PackageHatDao extends Repository<PackageHat, Integer> {
      Iterable<PackageHat> findAll();
//      @Query("select PackageHat from PackageHat where publisher.id = ?1")
      Iterable<PackageHat> findByPublisherId(Integer authorId);
      Optional<PackageHat> findByIdAndValid(Integer id, boolean valid);
      Optional<PackageHat> findById(Integer id);
      long count();
      PackageHat save (PackageHat entity);
      void delete (PackageHat entity);
      void deleteById(Integer id);
      void deleteByPublisher(PublisherInfo publisher);
      void deleteAll();
      boolean existsById(Integer id);
      boolean existsByPublisher(PublisherInfo publisher);

}
