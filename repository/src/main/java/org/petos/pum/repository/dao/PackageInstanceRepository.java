package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageInstance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface PackageInstanceRepository extends JpaRepository<PackageInstance, Long> {
List<PackageInstance> findAllByPackageInfoId(long id);

@EntityGraph(attributePaths = {"dependencies.dependency"})
List<PackageInstance> findWithDependenciesByPackageInfoId(long id);

Optional<PackageInstance> findByPackageInfoIdAndVersion(long id, String version);

@EntityGraph(attributePaths = {"dependencies.dependency", "archives.archiveType"})
Optional<PackageInstance> findWithDependenciesAndArchivesByPackageInfoIdAndVersion(long id, String version);
}