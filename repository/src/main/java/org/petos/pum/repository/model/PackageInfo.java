package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Entity
@IdClass(InstanceId.class)
@Table(name = "packages_info")
public class PackageInfo {
@Id
@Column(name = "package_id")
private Integer packageId;
@Id
@Column(name = "version_id")
private Integer versionId;
@Column(name = "version_label")
private String version;
@GeneratedValue(strategy = GenerationType.IDENTITY)
@CreatedDate
@Column(name = "create_time")
private Timestamp time;
@OneToOne(fetch = FetchType.LAZY)
@MapsId("licence_type")
private Licence licence; //replace LicenceId directly to Licence value

@ElementCollection
@CollectionTable(name = "DEPENDENCIES", joinColumns = {
    @JoinColumn(name = "PACKAGE_ID", referencedColumnName = "package_id"),
    @JoinColumn(name = "VERSION_ID", referencedColumnName = "version_id")
})

//@Columns(columns = {
//    @Column(name="DEPENDENCY_PACKAGE"),
//    @Column(name="DEPENDENCY_VERSION")
//})
private Set<DependencyId> dependencies;

//@ElementCollection
//@CollectionTable(name = "packages_payloads", joinColumns = {
//    @JoinColumn(name = "package_id", referencedColumnName = "package_id"),
//   @JoinColumn(name = "version_id", referencedColumnName = "version_id")
//})
//@Columns(columns = {
//    @Column(name="ARCHIVE_TYPE"),
//    @Column(name="PATH")
//})
@OneToMany
@JoinColumns({
    @JoinColumn(name = "package_id"),
    @JoinColumn(name = "version_id")
})
//@OneToMany(targetEntity = PayloadInstance.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
private List<PayloadInstance> payloads = new ArrayList<>();

public void addInstance(PayloadInstance instance) {
      payloads.add(instance);
}

public void removeInstance(PayloadInstance instance) {
      payloads.remove(instance);
}

public static PackageInfo valueOf(Integer packageId, Integer versionId) {
      PackageInfo info = new PackageInfo();
      InstanceId id = new InstanceId();
      id.setPackageId(packageId);
      id.setVersionId(versionId);
      return info;
}
}
