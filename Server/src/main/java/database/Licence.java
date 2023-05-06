package database;


import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
