package org.petos.pum.server.repositories;


import org.petos.pum.server.repositories.entities.PackageHat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.ParameterAccessor;

import java.util.List;
import java.util.Optional;

/**
 * This dao interact with PackageHat entity
 *
 * @author Paval Shlyk
 * @since 19/08/2023
 */
public interface PackageHatDao extends ListCrudRepository<PackageHat, Integer> {

Optional<PackageHat> findByIdAndValid(Integer id, boolean valid);

List<PackageHat> findAllByPublisherId(Integer id);

List<PackageHat> findAll();

List<PackageHat> findAnyByPublisherIdAndValid(Integer id, boolean valid);
//@Query("delete PackageHat where id = ?1 and valid = ?2")
void deleteAnyByPublisherIdAndValid(Integer id, boolean valid);
boolean existsAnyByPublisherId(Integer id);
public PackageHat getReferenceById(Integer id);
}
