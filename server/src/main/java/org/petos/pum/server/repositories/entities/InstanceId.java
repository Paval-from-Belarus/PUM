package org.petos.pum.server.repositories.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
@Data
@Embeddable
public class InstanceId implements Serializable {
@Column(name= "package_id")
private Integer packageId;
@Column(name = "version_id")
private Integer versionId;
public static InstanceId valueOf(Integer packageId, Integer versionId){
      var id = new InstanceId();
      id.setPackageId(packageId);
      id.setVersionId(versionId);
      return id;
}
}
