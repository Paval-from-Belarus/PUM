package org.petos.pum.server.repositories;

import org.hibernate.annotations.CompositeType;
import org.petos.pum.server.repositories.entities.DependencyId;
import org.petos.pum.server.repositories.entities.InstanceId;
import org.petos.pum.server.repositories.entities.PackageInfo;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * @author Paval Shlyk
 * @since 24/08/2023
 */
public interface PackageInfoDao extends Repository<PackageInfo, InstanceId> {
Iterable<PackageInfo> findPackageInfoByPackageId(Integer packageId);

Iterable<PackageInfo> findPackageInfoByPackageIdAndVersionIdBetween(Integer packageId, Integer startVersion, Integer endVersion);

Optional<PackageInfo> findPackageInfoByPackageIdAndVersionId(Integer packageId, Integer versionId);

PackageInfo save(PackageInfo info);

void deleteAllByPackageId(Integer packageId);

void deleteAllByPackageIdAndVersionId(Integer packageId, Integer versionId);
}
