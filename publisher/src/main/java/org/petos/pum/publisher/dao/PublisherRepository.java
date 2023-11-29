package org.petos.pum.publisher.dao;

import org.petos.pum.publisher.model.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
@Query("select Publisher from PublisherPackageInfo where id.packageId = ?")
Optional<Publisher> findByPackageId(long packageId);

Optional<Publisher> findByName(String name);
}