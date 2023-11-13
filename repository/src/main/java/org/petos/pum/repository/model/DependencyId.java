package org.petos.pum.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

//foreign key for collection table
@Data
@Embeddable
public class DependencyId implements Serializable {
@Column(name="DEPENDENCY_PACKAGE")
private Integer packageId;
@Column(name="DEPENDENCY_VERSION")
private Integer versionId;
private String version;
}
