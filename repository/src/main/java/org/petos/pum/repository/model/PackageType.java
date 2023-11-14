package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "package_type")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageType {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "package_type_id")
private long id;
@Column(name = "name")
private String name;
}
