package org.petos.pum.server.repositories.entities;

import lombok.*;


import jakarta.persistence.*;

import java.util.List;

//@SuppressWarnings("JpaDataSourceORMInspection")//foreign key for collection table
//@Embeddable
@Data
@Entity
@Table(name = "PACKAGES_PAYLOADS")
public class PayloadInstance {
@EmbeddedId
private InstanceId instanceId;
@Column(name = "path")
private String path;
@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "ARCHIVE_TYPE")
private Archive archiveType;
@ManyToOne(fetch = FetchType.LAZY)
private PackageInfo packageInfo;
}
