package org.petos.pum.repository.new_model;

import jakarta.persistence.*;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "package_alias")
public class PackageAlias {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "alias_id")
private long id;
@Column(name = "name")
private String name;
@ManyToOne(fetch = FetchType.LAZY)
private PackageInfo packageInfo;
}
