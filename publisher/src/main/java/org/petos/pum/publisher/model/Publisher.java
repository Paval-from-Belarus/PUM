package org.petos.pum.publisher.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@Entity
@Table(name = "publisher")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Publisher {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "publisher_id", nullable = false)
private long id;
@Column(name = "name", nullable = false)
private String name;
@Column(name = "email")
private String email;
@Column(name = "password", nullable = false)
private byte[] password;
@Column(name = "salt", nullable = false)
private byte[] salt;
}
