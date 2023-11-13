package org.petos.pum.server.repositories.entities;

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
@Table(name = "PUBLISHER_INFO")
public class PublisherInfo {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id")
private Integer id;
@Column(name = "author")
private String name;
@Column(name = "email")
private String email;
//@OneToMany(mappedBy = "publisher", cascade = CascadeType.ALL, orphanRemoval = true)
//private List<PackageHat> comments = new ArrayList<>();


public static PublisherInfo valueOf(String name) {
      PublisherInfo author = new PublisherInfo();
      author.name = name;
      author.email = "";
      return author;
}
}
