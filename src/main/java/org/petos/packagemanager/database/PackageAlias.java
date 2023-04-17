package org.petos.packagemanager.database;

import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "PACKAGES_ALIASES")
public class PackageAlias {
private String alias;
private Integer id;

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
