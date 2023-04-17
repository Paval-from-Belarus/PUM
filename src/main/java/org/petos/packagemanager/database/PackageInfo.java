package org.petos.packagemanager.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@IdClass(InstanceId.class)
@Table(name = "PACKAGES_INFO")
public class PackageInfo {
@Id
private Integer packageId;
@Id
private Integer versionId;
private String versionLabel;
private String payloadPath;
private Integer licence;
}
