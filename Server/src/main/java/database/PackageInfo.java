package database;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@IdClass(InstanceId.class)
@Table(name = "PACKAGES_INFO")
public class PackageInfo {
@Setter
@Getter
@Id
private Integer packageId;
@Setter
@Getter
@Id
private Integer versionId;
@Setter
@Getter
private String versionLabel;
@Setter
@Getter
private String payloadPath;
@GeneratedValue
@Getter
@Setter
@CreationTimestamp
private Timestamp time;
@Setter
@Getter
@ManyToOne
@JoinColumn(name = "licence")
private Licence licence; //replace LicenceId directly to Licence value

@Setter
@Getter
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

@Override
public boolean equals(Object object) {
      boolean response = false;
      if (object instanceof PackageInfo other) {
            response = other.packageId.compareTo(packageId) == 0 && other.versionId.compareTo(versionId) == 0;  
      }
      return response;
}
public static PackageInfo valueOf(Integer packageId, Integer versionId){
      PackageInfo info = new PackageInfo();
      info.setVersionId(versionId);
      info.setPackageId(packageId);
      return info;
}
}
