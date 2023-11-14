package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageAliasRepository extends JpaRepository<PackageAlias, Long> {
      Optional<PackageAlias> findByName(String name);
}