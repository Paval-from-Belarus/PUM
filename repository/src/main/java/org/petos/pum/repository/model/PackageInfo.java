package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "package_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageInfo {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "package_id")
private long id;
@NaturalId
@Column(name = "name")
private String name;
@Column(name = "status")
private long status;
@Column(name = "publisher_id")
private long publisherId;
@OneToMany(mappedBy = "packageInfo",
    cascade = CascadeType.ALL,
    orphanRemoval = true)
@Builder.Default
private List<PackageAlias> aliases = new ArrayList<>();
@ManyToOne
@JoinColumn(name = "license_id")
private License license;
@ManyToOne
@JoinColumn(name = "package_type_id")
private PackageType type;
}
