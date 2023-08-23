package org.petos.pum.server.repositories.entities;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
@Data
@Entity
@Table(name="ARCHIVES")
public class Archive {
@Id
@GeneratedValue
private Integer id;
private String type;
}
