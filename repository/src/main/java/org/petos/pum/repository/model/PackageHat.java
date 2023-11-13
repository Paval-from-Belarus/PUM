package org.petos.pum.repository.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Entity
@Table(name = "packages_hats")
public class PackageHat {
public static final boolean INVALID = false;
public static final boolean VALID = true;
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id", nullable = false)
private final Integer id;
@Column(name = "package_name", nullable = false)
private String name;
@Column(name = "valid")
private boolean valid;
@Column(name = "author_id")
private Integer publisherId;
//@ManyToOne(fetch = FetchType.LAZY)
//@JoinColumn(name = "author_id")
//private PublisherInfo publisher;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "package_type", nullable = false)
private final PackageType type;//the package without any type is impossible
//@OneToMany(mappedBy = "hat", orphanRemoval = true)
//@ElementCollection(targetClass = PackageAlias.class)
//@JoinColumn(name = "alias")
//@ElementCollection(fetch = FetchType.LAZY)
//@CollectionTable(name = "packages_aliases", joinColumns = @JoinColumn(name = "id"))
//@Column(name = "alias")
//private Set<String> aliases;
@OneToMany(mappedBy = "hat", orphanRemoval = true)
@Builder.Default
private List<PackageAlias> aliases = new ArrayList<>();

//@ElementCollection(fetch = FetchType.LAZY)
//@CollectionTable(name = "packages_aliases", joinColumns = @JoinColumn(name = "id"))
//@Column(name = "alias")
//private Set<String> aliases;
//public static PackageHat valueOf(String name, String[] aliases) {
//      PackageHat hat = new PackageHat();
//      hat.setName(name);
//      Set<String> set = Arrays.stream(aliases)
//			    .filter(alias -> !alias.isEmpty())
//			    .collect(Collectors.toSet());
//      hat.setAliases(set);
//      return hat;
//}
}
