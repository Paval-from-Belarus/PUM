package org.petos.pum.server.repositories;

import org.petos.pum.server.repositories.entities.PackageAlias;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 10/09/2023
 */
public interface PackageAliasDao extends Repository<PackageAlias, Long> {
PackageAlias save(PackageAlias alias);
Optional<PackageAlias> findByName(String name);
List<PackageAlias> findAll();

List<PackageAlias> findAllByHatId(Integer id);

void delete(PackageAlias alias);

void deleteAllByHatId(Integer id);

void deleteAll();

}
