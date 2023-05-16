package packages;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 *This class is used as upper interface for PackageStorage
 *To convert database representation to finite client
 */
@Accessors(prefix="")
public class PublishInstanceDTO extends AbstractDTO{
public static final String DEFAULT_LICENCE = "GNU";
@Getter
private final Integer packageId;
@Getter
private final String version;
@Getter
private final DependencyInfoDTO[] dependencies;
@Getter
@Setter
private String license;
@Getter
@Setter
private int payloadSize;

/**
 * @param dependencies is any alias (or package name) of dependency that should be used to install package.
 *                     Each dependency should be verified and exists other package will no publish.
 * @param version      is payload self
 * @param version      is unique String label for package Family
 */
public PublishInstanceDTO(Integer id, String version, DependencyInfoDTO[] dependencies) {
      this.packageId = id;
      this.version = version;
      this.dependencies = dependencies;
      this.payloadSize = 0;
      this.license = DEFAULT_LICENCE;
}
public String version() {
      return version;
}

public DependencyInfoDTO[] dependencies() {
      return dependencies;
}

public Integer packageId() {
      return packageId;
}
public static Optional<PublishInstanceDTO> valueOf(String content) {
      return valueOf(PublishInstanceDTO.class, content);
}

}
