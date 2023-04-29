package org.petos.packagemanager.database;

import org.hibernate.annotations.Columns;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

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
@GeneratedValue
private Timestamp time;
@ManyToOne
@JoinColumn(name = "licence")
private Licence licence; //replace LicenceId directly to Licence value

@ElementCollection
@CollectionTable(name = "DEPENDENCIES", joinColumns = {
    @JoinColumn(name = "PACKAGE_ID", referencedColumnName= "packageId"),
    @JoinColumn(name = "VERSION_ID", referencedColumnName= "versionId")
})
@Columns(columns = {
    @Column(name="DEPENDENCY_PACKAGE"),
    @Column(name="DEPENDENCY_VERSION")
})
private Set<DependencyId> dependencies;
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

public String getVersionLabel() {
      return versionLabel;
}

public void setVersionLabel(String versionLabel) {
      this.versionLabel = versionLabel;
}

public String getPayloadPath() {
      return payloadPath;
}

public void setPayloadPath(String payloadPath) {
      this.payloadPath = payloadPath;
}

public Licence getLicence() {
      return licence;
}
public void setLicence(Licence licence) {
      this.licence = licence;
}
public Set<DependencyId> getDependencies(){
      return dependencies;
}
public void setDependencies(Set<DependencyId> dependencies){
      this.dependencies= dependencies;
}
public static PackageInfo valueOf(Integer packageId, Integer versionId){
      PackageInfo info = new PackageInfo();
      info.setVersionId(versionId);
      info.setPackageId(packageId);
      return info;
}
}
