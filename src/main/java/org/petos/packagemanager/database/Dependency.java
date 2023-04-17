package org.petos.packagemanager.database;

import javax.persistence.Table;

@Table(name = "DEPENDENCIES")
public class Dependency {
private Integer packageId;
private Integer versionId;
private Integer dependencyPackage;
private Integer dependencyVersion;

public Integer getPackageId() {
      return packageId;
}

public void setPackageId(Integer packageId) {
      this.packageId = packageId;
}

public Integer getVersionId() {
      return versionId;
}

public void setVersionId(Integer versionId) {
      this.versionId = versionId;
}

public Integer getDependencyPackage() {
      return dependencyPackage;
}

public void setDependencyPackage(Integer dependencyPackage) {
      this.dependencyPackage = dependencyPackage;
}

public Integer getDependencyVersion() {
      return dependencyVersion;
}

public void setDependencyVersion(Integer dependencyVersion) {
      this.dependencyVersion = dependencyVersion;
}
}
