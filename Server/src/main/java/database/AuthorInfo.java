package database;

import javax.persistence.*;

@Entity
@Table(name="PUBLISHERS_INFO")
public class AuthorInfo {
@Id
@GeneratedValue
private int id;
private String name;
private String email;
private String hash;
private String salt;
public int getId() {
      return id;
}

public void setId(int id) {
      this.id = id;
}

public String getName() {
      return name;
}

public void setName(String name) {
      this.name = name;
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

public void setHash(String token) {
      this.hash = token;
}

public String getSalt() {
      return salt;
}

public void setSalt(String salt) {
      this.salt = salt;
}
public static AuthorInfo valueOf(String name, String hash, String salt) {
      AuthorInfo author = new AuthorInfo();
      author.name = name;
      author.hash = hash;
      author.salt = salt;
      author.email = "";
      return author;
}
}
