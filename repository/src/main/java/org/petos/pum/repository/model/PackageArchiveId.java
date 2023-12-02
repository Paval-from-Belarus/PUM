package org.petos.pum.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageArchiveId implements Serializable {
@Column(name = "package_instance_id")
private Long instanceId;
@Column(name = "archive_type_id")
private Short archiveTypeId;
}
