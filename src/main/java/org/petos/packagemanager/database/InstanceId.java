package org.petos.packagemanager.database;

import java.io.Serializable;

public class InstanceId implements Serializable {
private Integer packageId;
private Integer versionId;
public Integer getPackageId() {
      return packageId;
}

public Integer getVersionId() {
      return versionId;
}

public void setVersionId(Integer versionId) {
      this.versionId = versionId;
}

public void setPackageId(Integer packageId) {
      this.packageId = packageId;
}
public static InstanceId valueOf(Integer packageId, Integer versionId){
      var id = new InstanceId();
      id.setPackageId(packageId);
      id.setVersionId(versionId);
      return id;
}
}
