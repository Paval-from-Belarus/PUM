package org.petos.packagemanager.client.database;

import org.petos.packagemanager.packages.DependencyInfoDTO;

import java.util.Objects;

public class PackageInfo {
	public Integer packageId;
	public Integer versionId;
	public String license;
	public String payload;
	public String version;
	public DependencyInfoDTO[] dependencies;
	public boolean equals(Integer id, String version){
	      return Objects.equals(id, packageId) && Objects.equals(version, this.version);
	}
}
