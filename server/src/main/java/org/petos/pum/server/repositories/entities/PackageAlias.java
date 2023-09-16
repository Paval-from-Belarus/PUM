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
private String name;
//fetching alias obviously entails fetching package hat
//let's it will be a single sql request
@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
@JoinColumn(name = "hat_id")
private PackageHat hat;
}
