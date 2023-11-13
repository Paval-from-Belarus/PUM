package org.petos.pum.repository.new_model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 * @author Paval Shlyk
 * @since 03/11/2023
 */
@Embeddable
public class PackageInstanceId implements Serializable {
@Column(name = "package_id")
private long packageId;
@Column(name = "version_id")
private long versionId;
}
