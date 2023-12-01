package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageDependencyRepository extends JpaRepository<PackageDependency, Long> {
List<PackageDependency> findByTargetId(long id);

}