package org.petos.pum.publisher.model;

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
@MapsId("publisherId")
@ManyToOne(optional = false)
@JoinColumn(name = "publisher_id", referencedColumnName = "publisher_id")
private Publisher publisher;
}
