package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "package_instance")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageInstance {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "package_instance_id")
private long id;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "package_id", referencedColumnName = "package_id")
private PackageInfo packageInfo;
@OneToMany(mappedBy = "target", fetch = FetchType.LAZY,
    cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private List<PackageDependency> dependencies = new ArrayList<>();
@OneToMany(mappedBy = "instance", fetch = FetchType.LAZY,
    cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private List<PackageInstanceArchive> archives = new ArrayList<>();
@Column(name = "version")
private String version;
@CreationTimestamp
@Column(name = "publication_time")
private Date publicationTime;
}
