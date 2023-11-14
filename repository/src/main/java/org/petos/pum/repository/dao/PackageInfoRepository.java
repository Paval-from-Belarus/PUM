package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageInfoRepository extends JpaRepository<PackageInfo, Long> {
}