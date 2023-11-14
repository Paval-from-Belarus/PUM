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
@Table(name = "package_alias")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageAlias {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "alias_id")
private long id;
@Column(name = "name")
@NaturalId
private String name;
@ManyToOne(fetch = FetchType.LAZY)
private PackageInfo packageInfo;
}
