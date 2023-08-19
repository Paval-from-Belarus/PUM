package org.petos.pum.server.database;


import org.springframework.data.repository.Repository;

/**
 * @author Paval Shlyk
 * @since 19/08/2023
 */
public interface StorageDao<T, ID> extends Repository<T, ID> {

}
