package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Entity
@Table(name = "package_dependency")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageDependency {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "package_dependency_id")
private long dependencyId;
@ManyToOne
@JoinColumn(name = "target_package_instance_id", referencedColumnName = "package_instance_id")
private PackageInstance target;
@ManyToOne
@JoinColumn(name = "dependency_package_instance_id", referencedColumnName = "package_instance_id")
private PackageInstance dependency;
}
