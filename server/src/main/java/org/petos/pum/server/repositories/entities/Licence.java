package org.petos.pum.server.repositories.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "LICENCES")
public class Licence {
@Id
private Integer id;
private String name;

public String getName() {
      return name;
}

public void setName(String name) {
      this.name = name;
}

public Integer getId() {
      return id;
}

public void setId(Integer id) {
      this.id = id;
}
}
