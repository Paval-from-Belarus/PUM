package org.petos.pum.repository.new_model;

import jakarta.persistence.*;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "package_type")
public class PackageType {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "package_type_id")
private long id;
@Column(name = "name")
private String name;
}
