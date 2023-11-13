package org.petos.pum.server.repositories.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class InstanceId implements Serializable {
@Column(name= "package_id")
private Integer packageId;
@Column(name = "version_id")
private Integer versionId;

}
