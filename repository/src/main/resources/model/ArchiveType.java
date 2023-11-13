package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id", nullable = false)
private Integer id = null;
private String type = "";
}
