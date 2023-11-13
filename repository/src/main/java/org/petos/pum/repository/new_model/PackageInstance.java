package org.petos.pum.repository.new_model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

import java.util.Date;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Entity
@Table(name = "package_instance")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageInstance {
@EmbeddedId
private PackageInstanceId id;
@ManyToOne(fetch = FetchType.LAZY)
@MapsId("packageId")
private PackageInfo packageInfo;
@NaturalId
@Column(name = "version")
private String version;
@CreationTimestamp
@Column(name = "publication_time")
private Date publicationTime;
@Column(name = "payload_size")
private long payloadSize;
@Column(name = "payload_path")
private String payloadPath;
}
