package database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@SuppressWarnings("JpaDataSourceORMInspection")//foreign key for collection table
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DependencyId implements Serializable {
@Setter
@Getter
@Column(name="DEPENDENCY_PACKAGE")
private Integer packageId;
@Setter
@Getter
@Column(name="DEPENDENCY_VERSION")
private Integer versionId;

}
