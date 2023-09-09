package org.petos.pum.server.repositories;

import org.petos.pum.server.repositories.entities.PublisherInfo;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 24/08/2023
 */
public interface PublisherDao extends Repository<PublisherInfo, Integer> {
Optional<PublisherInfo> findById(Integer id);

}
