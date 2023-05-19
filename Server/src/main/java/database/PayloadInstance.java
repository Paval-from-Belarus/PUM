package database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.event.internal.EntityCopyAllowedLoggedObserver;

import javax.persistence.*;
@SuppressWarnings("JpaDataSourceORMInspection")//foreign key for collection table
@Embeddable
@Table(name="PACKAGES_PAYLOADS")
@AllArgsConstructor
@NoArgsConstructor
public class PayloadInstance {
@Setter @Getter
@ManyToOne
@JoinColumn(name="ARCHIVE_TYPE")
private Archive archiveType;
@Setter @Getter
private String path;
}
