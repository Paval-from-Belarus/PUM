package org.petos.pum.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@Entity
@Table(name = "repository_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryInfo {
@Id
private long id;//what is id for repository info? Repository info is aggregate relation
@Column(name = "packages_count")
private long packagesCount;
@Column(name = "repositry_name")
private String name;
@Column(name = "disk_space")
private long diskSpace;
}
