package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageInstance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageInstanceRepository extends JpaRepository<PackageInstance, Long> {
List<PackageInstance> findAllByPackageInfoId(long id);

@EntityGraph(attributePaths = {"dependencies.dependency"})
List<PackageInstance> findWithDependenciesByPackageInfoId(long id);

Optional<PackageInstance> findByPackageInfoIdAndVersion(long id, String version);

PackageInstance getByPackageInfoIdAndVersion(long id, String version);

@Deprecated//please, don't use such method because it cause exceptions)
@EntityGraph(attributePaths = {"dependencies.dependency", "archives.archiveType"})
Optional<PackageInstance> findWithDependenciesAndArchivesByPackageInfoIdAndVersion(long id, String version);

boolean existsByPackageInfoIdAndVersion(long id, String version);
}