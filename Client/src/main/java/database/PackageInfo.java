package database;

import packages.DependencyInfoDTO;

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
