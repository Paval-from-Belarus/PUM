package org.petos.packagemanager.database;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@SuppressWarnings("JpaDataSourceORMInspection")//foreign key for collection table
@Embeddable
public class DependencyId implements Serializable {
@Column(name="DEPENDENCY_PACKAGE")
private Integer packageId;
@Column(name="DEPENDENCY_VERSION")
private Integer versionId;

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
}
