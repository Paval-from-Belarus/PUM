package org.petos.pum.server.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Entity
@Table(name = "packages_aliases")
public class PackageAlias {
@Id
@Column(name = "id")
private final Long id;
@NaturalId
@Column(name = "alias")
private String alias;
@ManyToOne(fetch = FetchType.LAZY)//mapped by hat_id
@JoinColumn(name = "hat_id")
private PackageHat hat;
}
