package org.petos.pum.server.repositories.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "PACKAGES_TYPES")
public class PackageType {
public static final String APP = "Application";
public static final String LIBRARY = "Library";
public static final String DOCS = "Documentation";
@Id
@Column(name = "id")
private Integer id;
@Column(name = "type_name")
private String name;
}
