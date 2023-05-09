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
private String name;
private boolean valid;
@Column(name = "AUTHOR_ID")
private Integer authorId;
@ManyToOne
@JoinColumn(name = "payloadType")
private Payload payload;
@ElementCollection
@CollectionTable(name = "PACKAGES_ALIASES", joinColumns = @JoinColumn(name = "id"))
@Column(name = "alias")
private Set<String> aliases;



public String getName() {
      return name;
}

public void setName(String name) {
      this.name = name;
}

public Set<String> getAliases() {
      return aliases;
}

public void setAliases(Set<String> aliases) {
      this.aliases = aliases;
}

public Payload getPayload() {
      return payload;
}

public void setPayload(Payload payload) {
      this.payload = payload;
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

public boolean isValid() {
      return valid;
}

public void setValid(boolean valid) {
      this.valid = valid;
}

public Integer getAuthorId() {
      return authorId;
}

public void setAuthorId(Integer authorId) {
      this.authorId = authorId;
}
}
