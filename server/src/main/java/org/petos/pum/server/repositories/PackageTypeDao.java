package org.petos.pum.server.repositories;

import org.petos.pum.server.repositories.entities.PackageType;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 16/09/2023
 */
public interface PackageTypeDao extends Repository<PackageType, Integer> {
Optional<PackageType> findById(Integer id);
Optional<PackageType> findByName(String name);

List<PackageType> findAll();

long count();

PackageType save(PackageType type);

void delete(PackageType type);
}
