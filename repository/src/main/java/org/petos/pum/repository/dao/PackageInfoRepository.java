package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.PackageInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PackageInfoRepository extends JpaRepository<PackageInfo, Long> {
@EntityGraph(attributePaths = {"license", "type", "aliases"})
Optional<PackageInfo> findWithAliasesByIdAndStatus(long id, long status);

Optional<PackageInfo> findByPublisherIdAndName(long publisherId, String name);
}