package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Entity
@Table(name = "package_instance_archive")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageInstanceArchive {
@EmbeddedId
private PackageArchiveId id;
@MapsId("instanceId")
@JoinColumn(name = "package_instance_id", referencedColumnName = "package_instance_id")
@ManyToOne(fetch = FetchType.LAZY)
private PackageInstance instance;
@MapsId("archiveTypeId")
@JoinColumn(name = "archive_type_id", referencedColumnName = "archive_type_id")
@ManyToOne
private ArchiveType archiveType;
@Column(name = "payload_size")
private long payloadSize;
@Column(name = "payload_path")
private String payloadPath;
}
