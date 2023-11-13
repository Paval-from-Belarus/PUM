package org.petos.pum.repository.new_model;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "license")
public class License {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "license_id")
private long id;
@NaturalId
@Column(name = "name")
private String name;
}
