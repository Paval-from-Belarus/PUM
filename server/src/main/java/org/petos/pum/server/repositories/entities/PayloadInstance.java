package org.petos.pum.server.repositories.entities;

import lombok.*;


import jakarta.persistence.*;

//@SuppressWarnings("JpaDataSourceORMInspection")//foreign key for collection table
//@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@IdClass(InstanceId.class)
@Table(name = "packages_payloads")
public class PayloadInstance {
@Id
@Column(name = "package_id")
private Integer packageId;
@Id
@Column(name = "package_id")
private Integer versionId;
@Column(name = "path")
private String path = "";
@Column(name = "payload_size")
private Long payloadSize;
@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "archive_type")
private ArchiveType archiveType;
}
