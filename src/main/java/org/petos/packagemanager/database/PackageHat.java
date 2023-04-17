package org.petos.packagemanager.database;



import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PACKAGES_HATS")
public class PackageHat {
@Id
private Integer id;
private Integer payloadType;
private String name;

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
}
