package org.petos.pum.server.repositories.entities;

import lombok.*;

import jakarta.persistence.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ARCHIVES")
public class ArchiveType {
public static final String NONE = "NONE";
public static final String BROTLI = "BROTLI";
public static final String GZIP = "GZIP";
@Id
@GeneratedValue
@Column(name = "id", nullable = false)
private Integer id = null;
private String type = "";
}
