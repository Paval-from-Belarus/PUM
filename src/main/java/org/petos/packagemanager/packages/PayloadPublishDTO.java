package org.petos.packagemanager.packages;

import java.util.Objects;

/**
 *
 */
public final class PayloadPublishDTO {
private final byte[] payload;
private final String version;
private final String[] dependencies;
private String licenseType;

/**
 * @param dependencies is any alias (or package name) of dependency that should be used to install package.
 *                     Each dependency should be verified and exists other package will no publish.
 * @param payload      is payload self
 * @param version      is unique String label for package Family
 */
public PayloadPublishDTO(byte[] payload, String version, String[] dependencies) {
      this.payload = payload;
      this.version = version;
      this.dependencies = dependencies;
}

public byte[] payload() {
      return payload;
}

public String version() {
      return version;
}

public String[] dependencies() {
      return dependencies;
}

public String getLicenseType() {
      return licenseType;
}

public void setLicenseType(String licenseType) {
      this.licenseType = licenseType;
}
}
