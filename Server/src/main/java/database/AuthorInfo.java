package database;

import javax.persistence.*;

@Entity
@Table(name="PUBLISHERS_INFO")
public class AuthorInfo {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Integer id;
@Column(name="AUTHOR")
private String name;
private String email;
private String hash;
private String salt;
public static AuthorInfo valueOf(String name, String hash, String salt) {
      AuthorInfo author = new AuthorInfo();
      author.name = name;
      author.hash = hash;
      author.salt = salt;
      author.email = "";
      return author;
}

public int getId() {
      return id;
}

public void setId(int id) {
      this.id = id;
}

public String getName() {
      return name;
}

public void setName(String author) {
      this.name = author;
}

public String getEmail() {
      return email;
}

public void setEmail(String email) {
      this.email = email;
}

public String getHash() {
      return hash;
}

public void setHash(String hash) {
      this.hash = hash;
}

public String getSalt() {
      return salt;
}

public void setSalt(String salt) {
      this.salt = salt;
}
}
