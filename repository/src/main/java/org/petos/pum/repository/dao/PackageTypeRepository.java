package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageTypeRepository extends JpaRepository<PackageType, Long> {
PackageType getByName(String name);
}