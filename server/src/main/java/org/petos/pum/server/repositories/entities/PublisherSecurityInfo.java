package org.petos.pum.server.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * @author Paval Shlyk
 * @since 09/09/2023
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PUBLISHER_SECURITY_INFO")
public class PublisherSecurityInfo {
@Id
@Column(name = "id")
private Integer id;
@Column(name = "hash")
private String hash;
@Column(name = "salt")
private String salt;
}
