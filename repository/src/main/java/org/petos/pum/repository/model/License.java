package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "license")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class License {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "license_id")
private long id;
@NaturalId
@Column(name = "name")
private String name;
}
