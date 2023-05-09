package database;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "PACKAGES_HATS")
public class PackageHat {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Getter @Setter
private Integer id;
@Setter
@Getter
private String name;
@Setter
@Getter
private boolean valid;
@Setter
@Getter
@Column(name = "AUTHOR_ID")
private Integer authorId;
@Setter
@Getter
@ManyToOne
@JoinColumn(name = "payloadType")
private Payload payload;
@Setter
@Getter
@ElementCollection
@CollectionTable(name = "PACKAGES_ALIASES", joinColumns = @JoinColumn(name = "id"))
@Column(name = "alias")
private Set<String> aliases;


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
