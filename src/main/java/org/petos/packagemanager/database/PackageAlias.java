package org.petos.packagemanager.database;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name = "PACKAGES_ALIASES")
public class PackageAlias {
@Column(name="alias")
private String alias;
@Column(name="id")
private Integer id;
public PackageAlias(){}
public PackageAlias(Integer id, String alias){
      this.id = id;
      this.alias = alias;
}
public String getAlias() {
      return alias;
}

public void setAlias(String alias) {
      this.alias = alias;
}

public Integer getId() {
      return id;
}

public void setId(Integer id) {
      this.id = id;
}
}
