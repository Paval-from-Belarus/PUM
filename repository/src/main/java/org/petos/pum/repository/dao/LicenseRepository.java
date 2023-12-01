package org.petos.pum.repository.dao;

import org.petos.pum.repository.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
License getByName(String name);
}