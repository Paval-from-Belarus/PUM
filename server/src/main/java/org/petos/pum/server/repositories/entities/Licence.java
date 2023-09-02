package org.petos.pum.server.repositories.entities;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "LICENCES")
public class Licence {
public static final String MIT = "MIT";
public static final String GNU = "GNU";
public static final String APACHE = "Apache";
public static final String BEAR = "Bear";
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "id")
private Integer id;
@Column(name = "name")
private String name;
}
