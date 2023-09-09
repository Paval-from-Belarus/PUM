package org.petos.pum.server.repositories;

import org.petos.pum.server.repositories.entities.InstanceId;
import org.petos.pum.server.repositories.entities.PackageInfo;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 24/08/2023
 */
public interface PackageInfoDao extends Repository<PackageInfo, InstanceId> {
public static class CustomBean implements FactoryBean<Integer> {

      @Override
      public Integer getObject() throws Exception {
	    return null;
      }

      @Override
      public Class<?> getObjectType() {
	    return null;
      }
}
Iterable<PackageInfo> findPackageInfoByPackageId(Integer packageId);

Iterable<PackageInfo> findPackageInfoByPackageIdAndVersionIdBetween(Integer packageId, Integer startVersion, Integer endVersion);

Optional<PackageInfo> findPackageInfoByPackageIdAndVersionId(Integer packageId, Integer versionId);

PackageInfo save(PackageInfo info);

void deleteAllByPackageId(Integer packageId);

void deleteAllByPackageIdAndVersionId(Integer packageId, Integer versionId);
}
