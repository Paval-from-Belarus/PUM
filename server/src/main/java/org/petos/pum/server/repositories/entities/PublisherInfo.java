package org.petos.pum.server.repositories.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "PUBLISHERS_INFO")
public class PublisherInfo {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "id")
private Integer id;
@Column(name = "author")
private String name;
@Column(name = "email")
private String email;
@Column(name = "hash")
private String hash;
@Column(name = "salt")
private String salt;

public static PublisherInfo valueOf(String name, String hash, String salt) {
      PublisherInfo author = new PublisherInfo();
      author.name = name;
      author.hash = hash;
      author.salt = salt;
      author.email = "";
      return author;
}
}
