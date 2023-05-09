package packages;


/**
 *This class is used as upper interface for PackageStorage
 *To convert database representation to finite client
 */
public final class PackageInstanceDTO {
public static final String DEFAULT_LICENCE = "GNU";
private final Integer packageId;
private final String version;
private final DependencyInfoDTO[] dependencies;
private String license;
private int payloadSize;

/**
 * @param dependencies is any alias (or package name) of dependency that should be used to install package.
 *                     Each dependency should be verified and exists other package will no publish.
 * @param version      is payload self
 * @param version      is unique String label for package Family
 */
public PackageInstanceDTO(Integer id, String version, DependencyInfoDTO[] dependencies) {
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

public String getLicense() {
      return license;
}

public void setLicense(String license) {
      this.license = license;
}

public int getPayloadSize() {
      return payloadSize;
}

public void setPayloadSize(int payloadSize) {
      this.payloadSize = payloadSize;
}
}
