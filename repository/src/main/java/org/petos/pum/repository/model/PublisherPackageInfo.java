package org.petos.pum.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@Entity
@Table(name = "publisher_package_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublisherPackageInfo {
@EmbeddedId
private PublisherPackageInfoId id;
@MapsId("packageId")
@OneToOne(optional = false)
@JoinColumn(name = "package_id", referencedColumnName = "package_id")
private PackageInfo packageInfo;
}
