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
@Table(name = "archive_type")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArchiveType {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "archive_type_id")
private short id;
@Column(name = "name", nullable = false)
private String name;
}
