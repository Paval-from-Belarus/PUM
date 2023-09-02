package org.petos.pum.server.repositories.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "packages_hats")
public class PackageHat {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "id")
private Integer id;

@Column(name = "package_name")
private String name;
@Column(name = "valid")
private boolean valid;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id")
private PublisherInfo publisher;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "package_type")
private PackageType payload;

@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "packages_aliases", joinColumns = @JoinColumn(name = "id"))
@Column(name = "alias")
//@OneToMany(mappedBy = "alias", orphanRemoval = true)
private Set<String> aliases;

@Override
public String toString() {
      return "PackageHat: (id=" + id + ";name=" + name + ")";
}

public static PackageHat valueOf(String name, String[] aliases) {
      PackageHat hat = new PackageHat();
      hat.setName(name);
      Set<String> set = Arrays.stream(aliases)
			    .filter(alias -> !alias.isEmpty())
			    .collect(Collectors.toSet());
      hat.setAliases(set);
      return hat;
}
}
