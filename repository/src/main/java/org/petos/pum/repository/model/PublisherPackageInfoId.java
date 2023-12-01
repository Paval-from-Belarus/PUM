package org.petos.pum.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@Embeddable
public class PublisherPackageInfoId implements Serializable {
@Serial
private static final long serialVersionUID = 1;
@Column(name = "publisher_id", nullable = false)
private Long publisherId;
@Column(name = "package_id", nullable = false)
private Long packageId;
}
