package org.petos.packagemanager.database;



import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "PACKAGES_HATS")
public class PackageHat {
@Id
private Integer id;
private Integer payloadType;
private String name;
//@ElementCollection
//@CollectionTable(name = "PACKAGES_ALIASES")

@ElementCollection
@CollectionTable(name="PACKAGES_ALIASES", joinColumns = @JoinColumn(name="id"))
@Column(name="alias")
private Set<String> aliases;

public void setId(Integer id) {
      this.id = id;
}
public Integer getId() {
      return id;
}

public Integer getPayloadType() {
      return payloadType;
}

public void setPayloadType(Integer payloadType) {
      this.payloadType = payloadType;
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
}
