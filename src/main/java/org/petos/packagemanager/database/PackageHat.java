package org.petos.packagemanager.database;


import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "PACKAGES_HATS")
public class PackageHat {
@Id
@GeneratedValue(strategy = GenerationType.AUTO )
private Integer id;
private String name;
private boolean valid;
@ManyToOne
@JoinColumn(name= "payloadType")
private Payload payload;
@ElementCollection
@CollectionTable(name = "PACKAGES_ALIASES", joinColumns = @JoinColumn(name = "id"))
@Column(name = "alias")
private Set<String> aliases;

public void setId(Integer id) {
      this.id = id;
}

public Integer getId() {
      return id;
}


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
public void setAliases(String[] aliases){
      this.aliases = new HashSet<>();
      this.aliases.addAll(Arrays.asList(aliases));
}

public Payload getPayload() {
      return payload;
}
public void setPayload(Payload payload) {
      this.payload = payload;
}
public static PackageHat valueOf(String name, String[] aliases){
      PackageHat hat = new PackageHat();
      hat.setName(name);
      hat.setAliases(aliases);
      return hat;
}

public boolean isValid() {
      return valid;
}

public void setValid(boolean valid) {
      this.valid = valid;
}
}
