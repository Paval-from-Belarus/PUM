package org.petos.pum.server.repositories;

import org.petos.pum.server.repositories.entities.PayloadInstance;
import org.springframework.data.repository.Repository;

/**
 * @author Paval Shlyk
 * @since 10/09/2023
 */
public interface PayloadDao extends Repository<PayloadInstance, Integer> {


}
