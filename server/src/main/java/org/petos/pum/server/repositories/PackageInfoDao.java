package org.petos.pum.server.repositories;

import org.petos.pum.server.repositories.entities.InstanceId;
import org.petos.pum.server.repositories.entities.PackageInfo;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 24/08/2023
 */
public interface PackageInfoDao extends Repository<PackageInfo, InstanceId>, PagingAndSortingRepository<PackageInfo, InstanceId> {
Iterable<PackageInfo> findByPackageId(Integer packageId);

Optional<PackageInfo> findByPackageIdAndVersion(Integer packageId, String version);

PackageInfo save(PackageInfo info);

void deleteAllByPackageId(Integer packageId);

void deleteAllByPackageIdAndVersionId(Integer packageId, Integer versionId);
}
