package org.petos.pum.server.repositories.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "packages_aliases")
public class PackageAlias {
@Column(name="alias")
private String alias;
@Column(name="id")
private Integer id;
}
