package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
