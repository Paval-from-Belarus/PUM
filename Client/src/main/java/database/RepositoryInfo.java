package database;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.security.Timestamp;
@Accessors(chain = true)
public class RepositoryInfo {
public enum Status {Enabled, Disabled}
@Getter @Setter
private Integer packagesCnt;
@Getter @Setter
private String id; //the local id by which is possible to find repository
@Getter @Setter()
private String name;
/**
 * The time, after which the all info about repo should be updated
 * */
@Getter @Setter
private Long timeout; //in milliseconds
@Getter @Setter
public Long lastUpdate;
@Getter @Setter
private String baseUrl;
@Getter @Setter
private Status status;
@Getter @Setter
@Nullable
private byte[] publicKey; //key in form of base64 by which
@Getter @Setter
private String[] mirrors; //links by which this repo will be access too
}
