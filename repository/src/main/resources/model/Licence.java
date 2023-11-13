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
@Table(name = "LICENCES")
public class Licence {
public static final String MIT = "MIT";
public static final String GNU = "GNU";
public static final String APACHE = "Apache";
public static final String BEAR = "Bear";
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id", nullable = false)
private Integer id = null;
@Column(name = "name")
private String name = "";
}
