package org.petos.packagemanager.database;

import java.io.Serializable;

public class InstanceId implements Serializable {
private Integer packageId;
private Integer versionId;
public InstanceId(Integer packageId, Integer versionId){
      this.packageId = packageId;
      this.versionId = versionId;
}
}
