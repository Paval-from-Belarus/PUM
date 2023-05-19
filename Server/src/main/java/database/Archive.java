package database;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="ARCHIVES")
public class Archive {
@Id
@GeneratedValue
@Setter @Getter
private Integer id;
@Setter @Getter
private String type;
}
