package org.petos.pum.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Entity
@Table(name = "PACKAGES_TYPES")
public class PackageType {
public static final String APP = "Application";
public static final String LIBRARY = "Library";
public static final String DOCS = "Documentation";
@Id
@Column(name = "id")
private final Integer id;
@Column(name = "type_name")
private final String name;

}
