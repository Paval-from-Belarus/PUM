package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageArchiveId;
import org.petos.pum.repository.model.PackageInstanceArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageInstanceArchiveRepository extends JpaRepository<PackageInstanceArchive, PackageArchiveId> {
Optional<PackageInstanceArchive> findByInstanceIdAndArchiveTypeId(long instanceId, short archiveTypeId);
List<PackageInstanceArchive> findByInstanceId(long instanceId);
}